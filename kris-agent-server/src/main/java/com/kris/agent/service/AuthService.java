package com.kris.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kris.agent.config.JwtTokenProvider;
import com.kris.agent.dto.AuthResponse;
import com.kris.agent.dto.LoginRequest;
import com.kris.agent.entity.User;
import com.kris.agent.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public AuthResponse register(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null ||
                request.getUsername().trim().isEmpty() || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        if (request.getPassword().length() != 32) {
            throw new RuntimeException("密码格式错误");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("developer");
        userMapper.insert(user);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), "developer");
    }

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
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        String role = user.getRole() != null ? user.getRole() : "developer";
        return new AuthResponse(token, user.getId(), user.getUsername(), role);
    }
}
