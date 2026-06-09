package com.kris.agent.dto;

import lombok.Data;

/**
 * 调试请求 DTO
 *
 * 【前端类比】相当于前端调试 Skill/MCP 时提交的表单数据
 * 包含 Skill 和 MCP 两种调试场景的所有字段（字段可能部分为空）
 */
@Data
public class DebugRequest {
    private String name;
    private String skillType;
    private String runEnv;
    private String command;
    private String argsText;
    private String envText;
    private String toolCode;
    private String propertiesText;
    private String requiredText;
    private String promptContent;
    private String testMessage;
}
