package com.kris.agent.controller;

import com.kris.agent.dto.ChatRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.ChatService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public SseEmitter chat(@RequestBody ChatRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return chatService.streamChat(principal.getId(), request);
    }
}
