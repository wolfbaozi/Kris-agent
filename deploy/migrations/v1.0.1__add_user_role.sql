-- 版本: v1.0.1
-- 描述: 为 users 表添加 role 字段
-- 执行时机: 代码发版前，针对已运行的老数据库执行

ALTER TABLE users ADD COLUMN role ENUM('developer','product_manager') NOT NULL DEFAULT 'developer' AFTER password_hash;
