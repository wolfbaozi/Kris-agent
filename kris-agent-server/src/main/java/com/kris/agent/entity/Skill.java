package com.kris.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Skill 实体 —— 对应数据库 skills 表
 *
 * 【前端类比】相当于前端 types 里的 SkillConfig 接口
 * Skill 有两种类型：
 *   - tool: 函数式工具，需要 toolSchema（JSON Schema）和 toolCode（执行代码）
 *   - prompt: 纯文本指令，只需要 promptContent
 *
 * toolSchema/toolCode/promptContent 在数据库里都是 TEXT 类型，存 JSON 字符串或代码文本
 */
@Data
@TableName("skills")
public class Skill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private Integer isGlobal;

    private String skillType;

    private String sourceType;

    private String toolSchema;

    private String toolCode;

    private String promptContent;

    private String filePath;

    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
