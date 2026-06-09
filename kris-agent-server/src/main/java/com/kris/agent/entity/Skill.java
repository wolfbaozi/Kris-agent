package com.kris.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

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
