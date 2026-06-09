package com.kris.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件记录实体 —— 对应数据库 file_records 表
 *
 * 【前端类比】相当于前端文件上传后拿到的响应数据结构
 * originalName 是用户上传时的原始文件名，storedName 是后端生成的 UUID 文件名
 * 这样做是为了避免文件名冲突和路径注入攻击
 */
@Data
@TableName("file_records")
public class FileRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String originalName;

    private String storedName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
