package com.kris.agent.dto;

import lombok.Data;

/**
 * 登录请求 DTO
 *
 * 【前端类比】相当于前端登录表单提交的数据：{ username: string; password: string }
 * 注意：这里的 password 是前端已经 MD5 过的 32 位字符串，不是明文密码
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
