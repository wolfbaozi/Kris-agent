package com.kris.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kris.agent.config.EncryptionConfig;
import com.kris.agent.dto.ChatRequest;
import com.kris.agent.entity.ApiKey;
import com.kris.agent.entity.Skill;
import com.kris.agent.mapper.ApiKeyMapper;
import com.kris.agent.mapper.SkillMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天服务 —— AI 对话的核心逻辑
 *
 * 【前端类比】相当于前端的 useChat() composable 里调用 OpenAI API 的部分
 * 核心流程：
 *   1. 从数据库读取用户的 API Key（加密存储，需要解密）
 *   2. 构建 OpenAI 请求（system prompt + 用户消息 + Skill 注入）
 *   3. 流式调用 OpenAI API，逐块通过 SSE 推送给前端
 *
 * SseEmitter 相当于后端版的 EventSource：
 *   前端 EventSource 接收事件 -> 后端 SseEmitter 发送事件
 *   300000L = 5 分钟超时（AI 回答可能很慢）
 */
@Service
public class ChatService {

    private final ApiKeyMapper apiKeyMapper;
    private final SkillMapper skillMapper;
    private final EncryptionConfig encryptionConfig;
    private final McpRuntimeService mcpRuntimeService;

    @Value("${app.run-env:production}")
    private String currentEnv;

    public ChatService(ApiKeyMapper apiKeyMapper, SkillMapper skillMapper,
                       EncryptionConfig encryptionConfig, McpRuntimeService mcpRuntimeService) {
        this.apiKeyMapper = apiKeyMapper;
        this.skillMapper = skillMapper;
        this.encryptionConfig = encryptionConfig;
        this.mcpRuntimeService = mcpRuntimeService;
    }

    /**
     * 流式聊天
     *
     * 为什么用 new Thread() 异步执行？
     * 因为 SseEmitter 需要立即返回给前端（建立 SSE 连接），然后在后台线程里慢慢推数据
     * 【前端类比】相当于前端 await fetch() 后立即开始逐行读取 stream
     */
    public SseEmitter streamChat(Long userId, ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);

        new Thread(() -> {
            try {
                // 1. 获取用户的 API Key（解密）
                List<ApiKey> keys = apiKeyMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApiKey>()
                                .eq(ApiKey::getUserId, userId));
                if (keys.isEmpty()) {
                    sendEvent(emitter, "error", Collections.singletonMap("error", "请先配置 API Key"));
                    emitter.complete();
                    return;
                }
                ApiKey keyRec = keys.get(0);
                String apiKey = encryptionConfig.decrypt(keyRec.getEncryptedKey());
                String baseUrl = keyRec.getBaseUrl();
                String model = keyRec.getModel() != null && !keyRec.getModel().isEmpty()
                        ? keyRec.getModel() : "gpt-4o-mini";

                // 2. 构建 OpenAI 客户端（支持自定义 baseUrl，兼容第三方 API 代理）
                OpenAiService service = buildService(apiKey, baseUrl);

                // 3. 构建消息列表，注入 system prompt + Skill 内容
                List<ChatMessage> messages = request.getMessages().stream().map(m ->
                        new ChatMessage(m.getRole(), m.getContent())
                ).collect(Collectors.toList());

                String systemPrompt = "你是一个 AI Agent 学习助手。";
                String append = buildPromptAppend(userId, request.getSkillIds());
                if (append != null && !append.isEmpty()) {
                    systemPrompt += "\n\n" + append;
                }
                messages.add(0, new ChatMessage("system", systemPrompt));

                ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                        .model(model)
                        .messages(messages)
                        .build();

                // 4. 流式调用 OpenAI，每收到一个 chunk 就通过 SSE 推送给前端
                // blockingForEach 是阻塞式遍历（在异步线程里所以不会阻塞主线程）
                service.streamChatCompletion(completionRequest).blockingForEach(chunk -> {
                    String content = chunk.getChoices().get(0).getMessage().getContent();
                    if (content != null && !content.isEmpty()) {
                        sendEvent(emitter, "text-delta", Collections.singletonMap("content", content));
                    }
                });

                sendEvent(emitter, "done", new HashMap<>());
                emitter.complete();
            } catch (Exception e) {
                try {
                    sendEvent(emitter, "error", Collections.singletonMap("error", e.getMessage()));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 构建 OpenAI 服务客户端
     * 如果用户配了自定义 baseUrl（比如 API 代理），就用 Retrofit 手动构建
     * 【前端类比】相当于前端 new OpenAI({ baseURL: 'https://proxy.xxx.com' })
     */
    private OpenAiService buildService(String apiKey, String baseUrl) {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request request = chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer " + apiKey)
                                .build();
                        return chain.proceed(request);
                    })
                    .connectTimeout(Duration.ofSeconds(30))
                    .readTimeout(Duration.ofSeconds(300))
                    .build();

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(JacksonConverterFactory.create(mapper))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
            return new OpenAiService(retrofit.create(OpenAiApi.class));
        }
        return new OpenAiService(apiKey, Duration.ofSeconds(300));
    }

    /**
     * 构建 Skill 提示词追加内容
     * 把用户选中的 prompt 类型 Skill 内容拼接到 system prompt 里
     * 【前端类比】相当于前端组装请求参数时把额外配置合并进去
     */
    private String buildPromptAppend(Long userId, List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) return null;

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Skill> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(Skill::getEnabled, 1)
                .in(Skill::getId, skillIds)
                .and(w -> w.eq(Skill::getUserId, userId).or().eq(Skill::getIsGlobal, 1))
                .eq(Skill::getSkillType, "prompt");
        List<Skill> skills = skillMapper.selectList(wrapper);
        if (skills.isEmpty()) return null;

        return skills.stream()
                .map(s -> "## " + s.getName() + "\n" + s.getPromptContent())
                .collect(Collectors.joining("\n\n"));
    }

    private String buildMcpToolsDescription(Long userId, List<Long> mcpIds) {
        if (mcpIds == null || mcpIds.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        for (Long mcpId : mcpIds) {
            try {
                List<Map<String, Object>> tools = mcpRuntimeService.listTools(userId, mcpId);
                for (Map<String, Object> tool : tools) {
                    sb.append("- ").append(tool.get("name"));
                    if (tool.get("description") != null) {
                        sb.append(": ").append(tool.get("description"));
                    }
                    sb.append("\n");
                }
            } catch (Exception ignored) {}
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private void sendEvent(SseEmitter emitter, String type, Map<String, Object> data) throws IOException {
        Map<String, Object> event = new HashMap<>(data);
        event.put("type", type);
        emitter.send(SseEmitter.event().data(event));
    }
}
