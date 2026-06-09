package com.kris.agent.dto;

import lombok.Data;

import java.util.List;

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
