package com.kris.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体 —— 对应数据库 users 表
 *
 * 【前端类比】相当于 Prisma Schema 或 Sequelize Model 里的 User 定义
 * Entity 和数据库表是一一映射关系，每个字段对应表里的一列
 *
 * MyBatis-Plus 注解说明：
 * @TableName("users")  -> 指定对应的表名（类似 Prisma 的 @@map("users")）
 * @TableId(AUTO)       -> 主键，数据库自增（类似 Prisma 的 @id @default(autoincrement())）
 * @TableField(INSERT)  -> 插入时自动填充（类似 Prisma 的 @default(now())）
 *
 * 字段命名规则：Java 驼峰 -> 数据库下划线（自动转换，由 application.yml 配置）
 * passwordHash -> password_hash, createdAt -> created_at
 */
@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    private String role;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
