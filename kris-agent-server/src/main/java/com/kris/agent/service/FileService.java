package com.kris.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kris.agent.entity.FileRecord;
import com.kris.agent.mapper.FileRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 文件服务 —— 上传、列表、下载、删除
 *
 * 【前端类比】相当于前端的 useFiles() composable
 * 文件存储策略：
 *   - 原始文件名保留在数据库（展示给用户看）
 *   - 磁盘上用 UUID 重命名（避免冲突和安全问题）
 *   - 类似前端上传到 OSS 后存一个 URL 到数据库的思路
 */
@Service
public class FileService {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    private final FileRecordMapper fileRecordMapper;

    public FileService(FileRecordMapper fileRecordMapper) {
        this.fileRecordMapper = fileRecordMapper;
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException ignored) {
        }
    }

    /**
     * 上传流程：
     * 1. 提取文件扩展名
     * 2. 用 UUID 生成新文件名（避免重名覆盖）
     * 3. 写入磁盘
     * 4. 在数据库记录文件元信息
     */
    public FileRecord upload(Long userId, MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalName.substring(dotIndex).toLowerCase();
        }

        // UUID 重命名：abc.jpg -> 550e8400-e29b-41d4-a716-446655440000.jpg
        String storedName = UUID.randomUUID().toString() + ext;
        Path target = UPLOAD_DIR.resolve(storedName);
        file.transferTo(target.toFile());

        FileRecord record = new FileRecord();
        record.setUserId(userId);
        record.setOriginalName(originalName);
        record.setStoredName(storedName);
        record.setFilePath("/uploads/" + storedName);
        record.setFileSize(file.getSize());
        record.setFileType(file.getContentType());
        fileRecordMapper.insert(record);

        return record;
    }

    public List<FileRecord> list(Long userId) {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileRecord::getUserId, userId)
               .orderByDesc(FileRecord::getCreatedAt);
        return fileRecordMapper.selectList(wrapper);
    }

    /**
     * 删除：先删磁盘文件，再删数据库记录
     */
    public void delete(Long userId, Long fileId) throws IOException {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileRecord::getId, fileId).eq(FileRecord::getUserId, userId);
        FileRecord record = fileRecordMapper.selectOne(wrapper);
        if (record == null) {
            throw new RuntimeException("文件不存在");
        }

        Path filePath = UPLOAD_DIR.resolve(record.getStoredName());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        fileRecordMapper.deleteById(fileId);
    }

    public FileRecord getById(Long userId, Long fileId) {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileRecord::getId, fileId).eq(FileRecord::getUserId, userId);
        return fileRecordMapper.selectOne(wrapper);
    }
}
