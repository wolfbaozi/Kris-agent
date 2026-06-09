package com.kris.agent.dto;

import lombok.Data;

/**
 * Skill 配置请求 DTO
 *
 * 【前端类比】相当于前端 Skill 配置表单提交的数据结构
 * toolSchema 用 Object 类型，因为前端传的是 JSON 对象，Java 侧不做具体类型约束
 */
@Data
public class SkillRequest {
    private String name;
    private String skillType;
    private String sourceType;
    private Object toolSchema;
    private String toolCode;
    private String promptContent;
    private String filePath;
    private Integer isGlobal;
}
