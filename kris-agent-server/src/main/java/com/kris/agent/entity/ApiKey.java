package com.kris.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

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
