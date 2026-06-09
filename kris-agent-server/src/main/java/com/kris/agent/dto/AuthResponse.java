package com.kris.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录/注册响应 DTO
 *
 * 【前端类比】相当于前端登录后拿到的 response.data：{ token, userId, username, role }
 * @AllArgsConstructor 自动生成全参构造器，方便 Service 层直接 new AuthResponse(token, id, name, role)
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    private String role;
}
