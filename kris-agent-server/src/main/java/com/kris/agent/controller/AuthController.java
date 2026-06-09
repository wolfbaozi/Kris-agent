package com.kris.agent.controller;

import com.kris.agent.dto.AuthResponse;
import com.kris.agent.dto.LoginRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("用户名或密码错误")) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "未登录"));
        }
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Map<String, Object> info = new HashMap<>();
        info.put("userId", principal.getId());
        info.put("username", principal.getName());
        info.put("role", "developer");
        return ResponseEntity.ok(info);
    }
}
