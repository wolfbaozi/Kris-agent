package com.kris.agent.controller;

import com.kris.agent.dto.ChatRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.ChatService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天控制器 —— AI 对话 SSE 流式接口
 *
 * 【前端类比】相当于前端 EventSource / fetch + ReadableStream 对应的后端实现
 * SseEmitter 是 Spring 的 SSE（Server-Sent Events）推送工具
 * 【前端类比】相当于后端主动往客户端推数据，前端用 EventSource 或 fetch stream 接收
 *
 * 流程：Controller 返回 SseEmitter -> Service 在异步线程里不断 send 事件 -> 前端逐块接收
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * AI 对话接口 POST /api/chat
     * 返回 SseEmitter（不是普通 JSON），表示这是一个流式响应
     * 【前端类比】前端收到的是 ReadableStream，需要逐行解析 SSE 格式
     */
    @PostMapping
    public SseEmitter chat(@RequestBody ChatRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return chatService.streamChat(principal.getId(), request);
    }
}
