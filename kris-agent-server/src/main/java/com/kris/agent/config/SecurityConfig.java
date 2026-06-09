package com.kris.agent.config;

import com.kris.agent.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置
 *
 * 【前端类比】相当于前端路由守卫（Vue Router 的 beforeEach）+ axios 请求拦截器的后端版本
 * 它定义了：
 *   1. 哪些接口不需要登录就能访问（白名单）
 *   2. 哪些接口必须登录才能访问
 *   3. 用什么方式验证身份（JWT Token）
 *
 * @Configuration 表示这是一个配置类（类似前端的 vite.config.ts）
 * @Bean 表示把返回值注册到 Spring 容器中，全局可注入使用
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * 配置安全过滤链（Filter Chain）
     *
     * 【前端类比】相当于 Express 的中间件链 app.use(authMiddleware, routeHandler)
     * 请求按顺序经过每个 Filter，类似前端 axios 拦截器的链式调用
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                // 前后端分离项目禁用 CSRF（CSRF 是给传统表单用的）
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // 无状态会话：不创建 HttpSession，完全靠 JWT 验证身份
                // 【前端类比】相当于前端不用 Cookie Session，全靠 localStorage 里的 token
                .and()
                .authorizeHttpRequests()
                .antMatchers("/api/auth/login", "/api/auth/register", "/api/health").permitAll()
                // 白名单：登录、注册、健康检查不需要 Token
                .anyRequest().authenticated()
                // 其他所有接口都需要通过认证
                .and()
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
                // 在 Spring Security 默认的认证过滤器之前，插入我们的 JWT 过滤器
        return http.build();
    }

    /**
     * 密码编码器：BCrypt 哈希算法
     * 【前端类比】相当于前端的 crypto 库做密码哈希，但 BCrypt 是后端专用，自带盐值
     * 密码存储时：明文 -> BCrypt -> 哈希值存入数据库
     * 登录验证时：明文 + 数据库哈希值 -> BCrypt.matches() -> true/false
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
