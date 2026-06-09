package com.kris.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kris.agent.config.EncryptionConfig;
import com.kris.agent.dto.AiGenRequest;
import com.kris.agent.entity.ApiKey;
import com.kris.agent.mapper.ApiKeyMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AiGenService {

    private final ObjectMapper objectMapper;
    private final EncryptionConfig encryptionConfig;
    private final ApiKeyMapper apiKeyMapper;

    public AiGenService(ObjectMapper objectMapper, EncryptionConfig encryptionConfig,
                        ApiKeyMapper apiKeyMapper) {
        this.objectMapper = objectMapper;
        this.encryptionConfig = encryptionConfig;
        this.apiKeyMapper = apiKeyMapper;
    }

    public Map generateSkill(Long userId, AiGenRequest request) {
        String description = request.getDescription();
        if (description == null || description.trim().isEmpty()) {
            throw new RuntimeException("请提供 Skill 功能描述");
        }
        String userRole = request.getRole() != null ? request.getRole() : "developer";
        String roleGuide = "product_manager".equals(userRole)
                ? "用户是产品经理，可能不熟悉编程代码。请尽量用自然语言生成完整的配置。"
                : "用户是开发者，生成精准的技术配置。";

        String prompt = "你是一个 Skill 配置生成器。根据用户的自然语言描述，生成一个 Skill 配置。" + roleGuide + "\n\n"
                + "Skill 有两种类型：\n"
                + "1. tool - 函数式工具，需要定义参数 Schema 和执行代码\n"
                + "2. prompt - 纯文本指令，注入到系统提示词中\n\n"
                + "只返回纯 JSON，格式: {\"skillType\":\"tool或prompt\",\"name\":\"...\",...}\n\n"
                + "用户描述: " + description.trim();

        String jsonResult = callAi(userId, prompt);
        try {
            return objectMapper.readValue(jsonResult, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("AI 生成失败，返回格式不符");
        }
    }

    public Map generateMcp(Long userId, AiGenRequest request) {
        String description = request.getDescription();
        if (description == null || description.trim().isEmpty()) {
            throw new RuntimeException("请提供 MCP 功能描述");
        }
        String userRole = request.getRole() != null ? request.getRole() : "developer";
        String roleGuide = "product_manager".equals(userRole)
                ? "用户是产品经理，把配置名称做成中文友好的。"
                : "用户是开发者，生成精准的技术配置。";

        String prompt = "你是一个 MCP 配置生成器。" + roleGuide + "\n\n"
                + "只返回纯 JSON，格式: {\"name\":\"...\",\"runEnv\":\"all\",\"command\":\"...\",\"args\":[\"...\"],\"env\":{}}\n\n"
                + "用户描述: " + description.trim();

        String jsonResult = callAi(userId, prompt);
        try {
            return objectMapper.readValue(jsonResult, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("AI 生成失败，返回格式不符");
        }
    }

    private String callAi(Long userId, String prompt) {
        List<ApiKey> keys = apiKeyMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApiKey>()
                        .eq(ApiKey::getUserId, userId));
        if (keys.isEmpty()) {
            throw new RuntimeException("请先配置 API Key");
        }
        ApiKey keyRec = keys.get(0);
        String apiKey = encryptionConfig.decrypt(keyRec.getEncryptedKey());

        OpenAiService service = new OpenAiService(apiKey);
        ChatMessage message = new ChatMessage("user", prompt);
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(Collections.singletonList(message))
                .build();
        return service.createChatCompletion(completionRequest)
                .getChoices().get(0).getMessage().getContent();
    }
}
