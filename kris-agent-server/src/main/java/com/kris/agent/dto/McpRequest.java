package com.kris.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * MCP 服务器配置请求 DTO
 *
 * 【前端类比】相当于前端 MCP 配置表单提交的数据结构
 * args/env/config 用 Object 类型是因为前端可能传数组或对象，Java 统一用 Object 接收
 */
@Data
public class McpRequest {
    private String name;
    private String runEnv;
    private String sourceType;
    private String serverType;
    private String command;
    private List<String> args;
    private Object env;
    private String filePath;
    private Object config;
    private Integer isGlobal;
}
