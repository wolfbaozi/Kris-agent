package com.kris.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API Key 实体 —— 对应数据库 api_keys 表
 *
 * 【前端类比】相当于前端 types/chat.ts 里的 ApiKeyConfig 接口，但多了数据库映射
 * encryptedKey 存的是 AES-GCM 加密后的密文，不是明文 API Key
 */
@Data
@TableName("api_keys")
public class ApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String provider;

    private String encryptedKey;

    private String model;

    private String baseUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
