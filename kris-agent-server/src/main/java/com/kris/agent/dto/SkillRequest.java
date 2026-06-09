package com.kris.agent.dto;

import lombok.Data;

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
