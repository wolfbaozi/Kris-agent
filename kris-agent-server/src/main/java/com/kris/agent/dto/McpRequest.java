package com.kris.agent.dto;

import lombok.Data;

import java.util.List;

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
