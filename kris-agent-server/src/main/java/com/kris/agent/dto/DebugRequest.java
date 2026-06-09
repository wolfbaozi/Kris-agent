package com.kris.agent.dto;

import lombok.Data;

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
