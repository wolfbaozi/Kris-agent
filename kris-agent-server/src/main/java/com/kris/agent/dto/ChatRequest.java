package com.kris.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * 聊天请求 DTO
 *
 * 【前端类比】相当于前端 streamChat() 函数构造的请求体：
 * { messages: [{role, content}], keyId, mcpIds, skillIds }
 *
 * 内部类 ChatMessage 相当于前端的 interface ChatMessage { role: string; content: string }
 * Java 里用 static class 定义嵌套类型（类似 TypeScript 的 namespace 或嵌套 interface）
 */
@Data
public class ChatRequest {
    private List<ChatMessage> messages;
    private Long keyId;
    private List<Long> mcpIds;
    private List<Long> skillIds;

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
