package com.kris.agent.controller;

import com.kris.agent.dto.AiGenRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.AiGenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * AI 生成控制器 —— 用 AI 自动生成 Skill/MCP 配置
 *
 * 【前端类比】相当于前端的"AI 辅助配置"功能对应的后端接口
 * 用户输入自然语言描述 -> 后端调用 AI -> 返回 JSON 配置
 */
@RestController
@RequestMapping("/api/ai-gen")
public class AiGenController {

    private final AiGenService aiGenService;

    public AiGenController(AiGenService aiGenService) {
        this.aiGenService = aiGenService;
    }

    /**
     * AI 生成 Skill 配置 POST /api/ai-gen/skill
     */
    @PostMapping("/skill")
    public ResponseEntity generateSkill(@RequestBody AiGenRequest request, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(aiGenService.generateSkill(principal.getId(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * AI 生成 MCP 配置 POST /api/ai-gen/mcp
     */
    @PostMapping("/mcp")
    public ResponseEntity generateMcp(@RequestBody AiGenRequest request, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(aiGenService.generateMcp(principal.getId(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
