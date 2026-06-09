package com.kris.agent.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌工具类
 *
 * 【前端类比】相当于前端的 jsonwebtoken 库（js/jsonwebtoken），负责：
 *   - 签发 token（登录成功后给前端发 token）
 *   - 验证 token（前端每次请求带 token，后端校验是否合法）
 *   - 解析 token（从 token 中提取用户信息）
 *
 * JWT 结构：header.payload.signature（三段 Base64 用 . 连接）
 * payload 里存的就是用户数据（userId、username、过期时间等）
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 JWT Token
     * 【前端类比】相当于前端登录成功后调用 localStorage.setItem('token', xxx) 里的那个 xxx 的生成过程
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 验证 Token 是否合法（未过期 + 签名正确）
     * 【前端类比】相当于前端 axios 拦截器里判断 token 是否过期
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        return Long.parseLong(claims.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        return claims.get("username", String.class);
    }
}
