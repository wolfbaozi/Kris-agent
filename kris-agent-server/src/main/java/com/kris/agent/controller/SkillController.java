package com.kris.agent.controller;

import com.kris.agent.dto.SkillRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.SkillService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * Skill 管理控制器 —— CRUD + 启用/禁用
 *
 * 【前端类比】相当于前端 Skill 管理页面的后端接口
 * 结构与 McpController 完全一致（RESTful CRUD 的标准模式）
 */
@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ResponseEntity list(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(skillService.list(principal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity create(@RequestBody SkillRequest request, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Map result = skillService.create(principal.getId(), request);
            return ResponseEntity.status(201).body(result);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("已存在")) {
                return ResponseEntity.status(409).body(Collections.singletonMap("error", msg));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", msg));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity update(@PathVariable Long id, @RequestBody SkillRequest request,
                                  Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            skillService.update(principal.getId(), id, request);
            return ResponseEntity.ok(Collections.singletonMap("ok", true));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("已存在")) {
                return ResponseEntity.status(409).body(Collections.singletonMap("error", msg));
            }
            if (msg != null && msg.contains("不存在")) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", msg));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", msg));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            skillService.delete(principal.getId(), id);
            return ResponseEntity.ok(Collections.singletonMap("ok", true));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("不存在")) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", msg));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", msg));
        }
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity toggle(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(skillService.toggle(principal.getId(), id));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("不存在")) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", msg));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", msg));
        }
    }
}
