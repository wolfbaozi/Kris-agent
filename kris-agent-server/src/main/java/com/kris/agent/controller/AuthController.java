package com.kris.agent.controller;

import com.kris.agent.dto.AuthResponse;
import com.kris.agent.dto.LoginRequest;
import com.kris.agent.security.UserPrincipal;
import com.kris.agent.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 —— 处理登录、注册、获取当前用户
 *
 * 【前端类比】相当于 Express/Koa 里的路由定义：
 *   router.post('/api/auth/login', loginHandler)
 *   router.post('/api/auth/register', registerHandler)
 *   router.get('/api/auth/me', meHandler)
 *
 * @RestController = @Controller + @ResponseBody
 *   表示这个类里所有方法的返回值直接序列化为 JSON（不是渲染模板）
 *   【前端类比】相当于 Express 里 res.json(result)
 *
 * @RequestMapping("/api/auth") 统一前缀，类里所有接口都以 /api/auth 开头
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 构造器注入（Spring 推荐的依赖注入方式）
     * 【前端类比】相当于在构造函数里接收外部传入的 service 实例
     * Spring 会自动从容器中找到 AuthService 的实例注入进来（IoC 控制反转）
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册接口 POST /api/auth/register
     * @RequestBody 把请求体的 JSON 自动反序列化为 LoginRequest 对象
     * 【前端类比】相当于 req.body 被解析成 TypeScript 类型
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * 登录接口 POST /api/auth/login
     * ResponseEntity<?> 是 Spring 的响应包装器，可以控制状态码、headers、body
     * 【前端类比】相当于 res.status(401).json({ error: "..." })
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("用户名或密码错误")) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * 获取当前用户信息 GET /api/auth/me
     * Authentication 参数由 Spring Security 自动注入（经过 JWT 过滤器后设置的用户上下文）
     * 【前端类比】相当于前端从 store.currentUser 获取当前登录用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "未登录"));
        }
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Map<String, Object> info = new HashMap<>();
        info.put("userId", principal.getId());
        info.put("username", principal.getName());
        info.put("role", "developer");
        return ResponseEntity.ok(info);
    }
}
