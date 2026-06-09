package com.kris.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Path UPLOAD_DIR = Paths.get("uploads");
    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(
            ".js", ".json", ".md", ".txt", ".yml", ".yaml", ".mjs", ".ts"));

    public FileController() {
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException ignored) {
        }
    }

    @PostMapping
    public ResponseEntity upload(@RequestParam("file") MultipartFile file,
                                  Authentication authentication) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(errorMap("请选择要上传的文件"));
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorMap("文件名不能为空"));
        }
        int dotIndex = originalName.lastIndexOf('.');
        String ext = dotIndex >= 0 ? originalName.substring(dotIndex).toLowerCase() : "";
        if (!ALLOWED_EXT.contains(ext)) {
            return ResponseEntity.badRequest().body(errorMap("不支持的文件类型"));
        }
        try {
            String uniqueName = System.currentTimeMillis() + "-" +
                    (int) (Math.random() * 1e9) + "-" + originalName;
            Path target = UPLOAD_DIR.resolve(uniqueName);
            file.transferTo(target.toFile());

            byte[] bytes = Files.readAllBytes(target);
            String raw = new String(bytes, StandardCharsets.UTF_8);

            Map<String, Object> result = new HashMap<>();
            result.put("id", uniqueName);
            result.put("name", originalName);
            result.put("size", file.getSize());
            result.put("path", "/uploads/" + uniqueName);
            result.put("ext", ext);
            if (".json".equals(ext)) {
                result.put("data", new ObjectMapper().readTree(raw));
            } else {
                result.put("data", raw);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorMap("文件解析失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{filename}")
    public ResponseEntity download(@PathVariable String filename,
                                    Authentication authentication) {
        Path filePath = UPLOAD_DIR.resolve(filename);
        if (!Files.exists(filePath)) {
            return ResponseEntity.status(404).body(errorMap("文件不存在"));
        }
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            return ResponseEntity.ok(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(errorMap("读取文件失败"));
        }
    }

    private Map<String, Object> errorMap(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("error", msg);
        return map;
    }
}
