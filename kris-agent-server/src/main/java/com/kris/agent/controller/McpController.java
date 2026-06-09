package com.kris.agent.controller;

import com.kris.agent.dto.McpRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.McpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/mcps")
public class McpController {

    private final McpService mcpService;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    @GetMapping
    public ResponseEntity list(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(mcpService.list(principal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

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
