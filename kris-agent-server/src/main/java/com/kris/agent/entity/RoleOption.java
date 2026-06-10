package com.kris.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role_options")
public class RoleOption {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String roleKey;

    private String roleLabel;

    private String roleDesc;

    private Integer sortOrder;

    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
