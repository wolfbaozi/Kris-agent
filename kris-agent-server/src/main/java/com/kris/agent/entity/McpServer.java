package com.kris.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

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
