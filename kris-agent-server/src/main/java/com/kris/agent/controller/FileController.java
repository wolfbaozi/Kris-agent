package com.kris.agent.controller;

import com.kris.agent.entity.FileRecord;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 文件管理控制器 —— 上传、列表、下载、删除
 *
 * 【前端类比】相当于前端文件管理功能的后端接口
 * MultipartFile 是 Spring 对 multipart/form-data 的封装
 * 【前端类比】相当于前端 FormData 里 append('file', file) 的那个 file
 *
 * 下载接口用 Resource + APPLICATION_OCTET_STREAM 返回二进制流
 * 【前端类比】相当于前端创建 Blob URL 触发下载
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 上传文件 POST /api/files
     * @RequestParam("file") 从 form-data 中取名为 file 的字段
     * 【前端类比】相当于前端 FormData.get('file')
     */
    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    Authentication authentication) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(errorMap("请选择要上传的文件"));
        }
        try {
            Long userId = getUserId(authentication);
            FileRecord record = fileService.upload(userId, file);
            return ResponseEntity.ok(record);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(errorMap("文件上传失败: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<FileRecord> records = fileService.list(userId);
        return ResponseEntity.ok(records);
    }

    /**
     * 下载文件 GET /api/files/{id}/download
     * Content-Disposition: attachment 告诉浏览器这是下载文件（不是在线预览）
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id,
                                      Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            FileRecord record = fileService.getById(userId, id);
            if (record == null) {
                return ResponseEntity.status(404).body(errorMap("文件不存在"));
            }

            Path filePath = Paths.get("uploads").resolve(record.getStoredName());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.status(404).body(errorMap("文件不存在"));
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.getOriginalName() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(errorMap("下载失败"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            fileService.delete(userId, id);
            return ResponseEntity.ok(Collections.singletonMap("message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorMap(e.getMessage()));
        }
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("未登录");
        }
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }

    private Map<String, Object> errorMap(String msg) {
        return Collections.singletonMap("error", msg);
    }
}
