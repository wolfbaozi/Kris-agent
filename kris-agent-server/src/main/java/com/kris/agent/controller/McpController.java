package com.kris.agent.controller;

import com.kris.agent.dto.McpRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.McpRuntimeService;
import com.kris.agent.service.McpService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mcps")
public class McpController {

    private final McpService mcpService;
    private final McpRuntimeService mcpRuntimeService;

    public McpController(McpService mcpService, McpRuntimeService mcpRuntimeService) {
        this.mcpService = mcpService;
        this.mcpRuntimeService = mcpRuntimeService;
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

    @GetMapping("/quota")
    public ResponseEntity quota(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(mcpRuntimeService.checkQuota(principal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/running-status")
    public ResponseEntity runningStatus(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(mcpRuntimeService.getRunningStatus(principal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity create(@RequestBody McpRequest request, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Map<String, Object> quota = mcpRuntimeService.checkQuota(principal.getId());
            if (!(Boolean) quota.get("canCreate")) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "已达到最大MCP配置数量限制"));
            }
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

    @PostMapping("/ai-create")
    public ResponseEntity aiCreate(@RequestBody Map<String, String> body, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Map<String, Object> quota = mcpRuntimeService.checkQuota(principal.getId());
            if (!(Boolean) quota.get("canCreate")) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "已达到最大MCP配置数量限制"));
            }
            String description = body.get("description");
            String role = body.get("role");
            Map result = mcpService.createFromDescription(principal.getId(), description, role);
            return ResponseEntity.status(201).body(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "role", required = false) String role,
                                  Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Map<String, Object> quota = mcpRuntimeService.checkQuota(principal.getId());
            if (!(Boolean) quota.get("canCreate")) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "已达到最大MCP配置数量限制"));
            }
            String content = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            Map result = mcpService.createFromFile(principal.getId(), file.getOriginalFilename(), content, role);
            return ResponseEntity.status(201).body(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "文件读取失败"));
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

    @PostMapping("/{id}/start")
    public ResponseEntity start(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            mcpRuntimeService.startMcp(principal.getId(), id);
            return ResponseEntity.ok(Collections.singletonMap("ok", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity stop(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            mcpRuntimeService.stopMcp(principal.getId(), id);
            return ResponseEntity.ok(Collections.singletonMap("ok", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/tools")
    public ResponseEntity listTools(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(mcpRuntimeService.listTools(principal.getId(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/tools/{toolName}/call")
    public ResponseEntity callTool(@PathVariable Long id, @PathVariable String toolName,
                                    @RequestBody Map<String, Object> arguments,
                                    Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(mcpRuntimeService.callTool(principal.getId(), id, toolName, arguments));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Map<String, Object> data = mcpService.export(principal.getId(), id);
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(data);
            String filename = data.get("name") + ".mcp.json";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("不存在")) {
                return ResponseEntity.status(404).body(null);
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
