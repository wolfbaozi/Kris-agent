package com.kris.agent.dto;

import lombok.Data;

/**
 * 登录/注册请求 DTO
 *
 * 【前端类比】相当于前端登录表单提交的数据
 * role 字段仅在注册时使用，登录时忽略
 * 可选值：developer（开发者）、product_manager（产品经理）、designer（设计师）
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
    private String role;
}
