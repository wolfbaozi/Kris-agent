package com.kris.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kris.agent.config.JwtTokenProvider;
import com.kris.agent.dto.AuthResponse;
import com.kris.agent.dto.LoginRequest;
import com.kris.agent.entity.User;
import com.kris.agent.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务 —— 处理注册、登录的核心业务逻辑
 *
 * 【前端类比】相当于前端的 useAuth() composable 里的 register() 和 login() 函数
 * Service 层负责：参数校验 -> 业务逻辑 -> 操作数据库 -> 返回结果
 * Controller 只做"接收请求 -> 调用 Service -> 返回响应"，不包含业务逻辑
 *
 * @Service 把这个类注册为 Spring Bean，可以被 Controller 通过构造器注入使用
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 注册流程：
     * 1. 校验参数（类似前端的表单验证）
     * 2. 检查用户名是否重复（类似前端的唯一性校验）
     * 3. 密码加密后存入数据库（类似前端把数据存到后端）
     * 4. 生成 JWT Token 返回（类似前端登录成功后存 token）
     */
    public AuthResponse register(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null ||
                request.getUsername().trim().isEmpty() || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        if (request.getPassword().length() != 32) {
            throw new RuntimeException("密码格式错误");
        }
        String role = normalizeRole(request.getRole());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        userMapper.insert(user);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), role);
    }

    private String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) return "developer";
        switch (role) {
            case "product_manager":
            case "designer":
            case "developer":
                return role;
            default:
                return "developer";
        }
    }

    /**
     * 登录流程：
     * 1. 根据用户名查找用户（类似前端 findByUsername）
     * 2. 验证密码（BCrypt.matches 比对哈希值）
     * 3. 生成 Token 返回
     */
    public AuthResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null ||
                request.getUsername().trim().isEmpty() || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        if (request.getPassword().length() != 32) {
            throw new RuntimeException("密码格式错误");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        // matches() 把明文和数据库里的哈希值做比对
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        String role = user.getRole() != null ? user.getRole() : "developer";
        return new AuthResponse(token, user.getId(), user.getUsername(), role);
    }
}
