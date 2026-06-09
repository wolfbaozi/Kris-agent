package com.kris.agent.controller;

import com.kris.agent.dto.ApiKeyRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * API Key 管理控制器 —— CRUD
 *
 * 【前端类比】相当于前端"API Key 设置"页面的后端接口
 * 注意：列表接口不返回真实的 API Key（已加密存储），只返回 provider/model/baseUrl 等元信息
 */
@RestController
@RequestMapping("/api/apikeys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(apiKeyService.list(principal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ApiKeyRequest request, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            apiKeyService.create(principal.getId(), request);
            return ResponseEntity.status(201).body(Collections.singletonMap("ok", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ApiKeyRequest request,
                                     Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            apiKeyService.update(principal.getId(), id, request);
            return ResponseEntity.ok(Collections.singletonMap("ok", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            apiKeyService.delete(principal.getId(), id);
            return ResponseEntity.ok(Collections.singletonMap("ok", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
