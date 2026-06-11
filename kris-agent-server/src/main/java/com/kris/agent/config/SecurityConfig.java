package com.kris.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kris.agent.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.HashMap;
import java.util.Map;

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
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests()
                .antMatchers("/api/auth/login", "/api/auth/register", "/api/health").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    Map<String, Object> body = new HashMap<>();
                    body.put("error", "登录已过期，请重新登录");
                    new ObjectMapper().writeValue(response.getOutputStream(), body);
                })
                .and()
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
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
