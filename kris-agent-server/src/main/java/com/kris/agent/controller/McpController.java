package com.kris.agent.controller;

import com.kris.agent.dto.McpRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.McpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * MCP 服务器管理控制器 —— CRUD + 启用/禁用
 *
 * 【前端类比】相当于前端 MCP 管理页面的后端接口
 * RESTful 风格：GET 列表、POST 创建、PUT 更新、DELETE 删除、PATCH 切换状态
 *
 * @PathVariable 从 URL 路径中取值，如 /api/mcps/{id} 里的 id
 * 【前端类比】相当于 Express 的 req.params.id 或 Vue Router 的 route.params.id
 */
@RestController
@RequestMapping("/api/mcps")
public class McpController {

    private final McpService mcpService;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    /**
     * 获取 MCP 列表 GET /api/mcps
     */
    @GetMapping
    public ResponseEntity list(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(mcpService.list(principal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * 创建 MCP POST /api/mcps
     * 409 Conflict 表示名称重复（类似前端接口返回的"已存在"错误）
     */
    @PostMapping
    public ResponseEntity create(@RequestBody McpRequest request, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Map result = mcpService.create(principal.getId(), request);
            return ResponseEntity.status(201).body(result);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("已存在")) {
                return ResponseEntity.status(409).body(Collections.singletonMap("error", msg));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", msg));
        }
    }

    /**
     * 更新 MCP PUT /api/mcps/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity update(@PathVariable Long id, @RequestBody McpRequest request,
                                  Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            mcpService.update(principal.getId(), id, request);
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

    /**
     * 删除 MCP DELETE /api/mcps/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            mcpService.delete(principal.getId(), id);
            return ResponseEntity.ok(Collections.singletonMap("ok", true));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("不存在")) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", msg));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", msg));
        }
    }

    /**
     * 切换启用/禁用 PATCH /api/mcps/{id}/toggle
     * PATCH 用于部分更新（只改 enabled 字段），PUT 用于全量更新
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity toggle(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(mcpService.toggle(principal.getId(), id));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("不存在")) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", msg));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", msg));
        }
    }
}
