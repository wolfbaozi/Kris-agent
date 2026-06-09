package com.kris.agent.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

/**
 * 用户主体对象
 *
 * 【前端类比】相当于前端 store 里的 currentUser 对象 { id, name }
 * 在 JWT 过滤器验证通过后，会被设置到 Spring Security 上下文中
 * Controller 里通过 Authentication.getPrincipal() 获取当前登录用户
 *
 * Lombok 注解说明：
 * @Getter     -> 自动生成 getId()、getName()（类似 TypeScript 的 readonly 属性）
 * @AllArgsConstructor -> 自动生成全参构造器（不用手写 constructor）
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements Principal {
    private Long id;
    private String name;
}
