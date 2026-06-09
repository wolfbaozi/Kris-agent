package com.kris.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 服务器实体 —— 对应数据库 mcp_servers 表
 *
 * 【前端类比】相当于前端 types 里的 McpConfig 接口
 * args/env/config 在数据库里存的是 JSON 字符串（TEXT 类型）
 * 读取时需要用 ObjectMapper 反序列化为 Java 对象（类似 JSON.parse()）
 */
@Data
@TableName("mcp_servers")
public class McpServer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private Integer isGlobal;

    private String runEnv;

    private String sourceType;

    private String serverType;

    private String command;

    private String args;

    private String env;

    private String filePath;

    private Integer enabled;

    private String config;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
