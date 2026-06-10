package com.kris.agent.controller;

import com.kris.agent.dto.SkillRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.SkillService;
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

    /**
     * AI 描述创建 POST /api/skills/ai-create
     * 接受自然语言描述，AI 自动生成 Skill 配置并保存
     * 适用于产品经理等不写代码的角色
     */
    @PostMapping("/ai-create")
    public ResponseEntity aiCreate(@RequestBody Map<String, String> body, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String description = body.get("description");
            String role = body.get("role");
            Map result = skillService.createFromDescription(principal.getId(), description, role);
            return ResponseEntity.status(201).body(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * 文件上传创建 POST /api/skills/upload
     * JSON 文件 -> 直接解析为 Skill 配置
     * 文本文件（.txt/.md） -> 当作描述，AI 生成配置
     */
    @PostMapping("/upload")
    public ResponseEntity upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "role", required = false) String role,
                                  Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String content = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            Map result = skillService.createFromFile(principal.getId(), file.getOriginalFilename(), content, role);
            return ResponseEntity.status(201).body(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "文件读取失败"));
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

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id, Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Map<String, Object> data = skillService.export(principal.getId(), id);
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(data);
            String filename = data.get("name") + ".skill.json";
            
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
