package com.kris.agent.controller;

import com.kris.agent.dto.AiGenRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.AiGenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/ai-gen")
public class AiGenController {

    private final AiGenService aiGenService;

    public AiGenController(AiGenService aiGenService) {
        this.aiGenService = aiGenService;
    }

    @PostMapping("/skill")
    public ResponseEntity generateSkill(@RequestBody AiGenRequest request, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(aiGenService.generateSkill(principal.getId(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

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
