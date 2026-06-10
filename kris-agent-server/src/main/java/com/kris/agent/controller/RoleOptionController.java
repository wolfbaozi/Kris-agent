package com.kris.agent.controller;

import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.RoleOptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role-options")
public class RoleOptionController {

    private final RoleOptionService roleOptionService;

    public RoleOptionController(RoleOptionService roleOptionService) {
        this.roleOptionService = roleOptionService;
    }

    @GetMapping
    public ResponseEntity list() {
        List<Map<String, Object>> options = roleOptionService.list();
        return ResponseEntity.ok(options);
    }

    @PostMapping
    public ResponseEntity create(@RequestBody Map<String, Object> body, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Map result = roleOptionService.create(body);
            return ResponseEntity.status(201).body(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity update(@PathVariable Integer id, @RequestBody Map<String, Object> body,
                                  Authentication authentication) {
        try {
            roleOptionService.update(id, body);
            return ResponseEntity.ok(Collections.singletonMap("ok", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable Integer id, Authentication authentication) {
        try {
            roleOptionService.delete(id);
            return ResponseEntity.ok(Collections.singletonMap("ok", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
