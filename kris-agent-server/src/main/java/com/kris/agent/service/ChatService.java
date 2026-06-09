package com.kris.agent.service;

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

@Service
public class ChatService {

    private final ApiKeyMapper apiKeyMapper;
    private final SkillMapper skillMapper;
    private final EncryptionConfig encryptionConfig;

    @Value("${app.run-env:production}")
    private String currentEnv;

    public ChatService(ApiKeyMapper apiKeyMapper, SkillMapper skillMapper,
                       EncryptionConfig encryptionConfig) {
        this.apiKeyMapper = apiKeyMapper;
        this.skillMapper = skillMapper;
        this.encryptionConfig = encryptionConfig;
    }

    public SseEmitter streamChat(Long userId, ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);

        new Thread(() -> {
            try {
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

                OpenAiService service = buildService(apiKey, baseUrl);

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

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(JacksonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
            return new OpenAiService(retrofit.create(OpenAiApi.class));
        }
        return new OpenAiService(apiKey, Duration.ofSeconds(300));
    }

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

    private void sendEvent(SseEmitter emitter, String type, Map<String, Object> data) throws IOException {
        Map<String, Object> event = new HashMap<>(data);
        event.put("type", type);
        emitter.send(SseEmitter.event().data(event));
    }
}
