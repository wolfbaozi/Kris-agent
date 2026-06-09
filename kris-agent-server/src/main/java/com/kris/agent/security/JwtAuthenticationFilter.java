package com.kris.agent.security;

import com.kris.agent.config.JwtTokenProvider;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 *
 * 【前端类比】相当于前端 axios 的请求拦截器（request interceptor），但方向相反：
 *   - 前端拦截器：请求发出前，自动往 headers 里塞 token
 *   - 后端过滤器：请求到达 Controller 前，自动从 headers 里取 token 并验证
 *
 * 执行流程（类似 Express 的 middleware）：
 *   请求进入 -> 提取 Authorization header -> 验证 JWT -> 设置用户上下文 -> 放行到 Controller
 *
 * OncePerRequestFilter 保证每个请求只经过此过滤器一次（避免重定向时重复执行）
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // Token 合法，解析出用户信息并设置到 Spring Security 上下文中
            // 【前端类比】相当于把用户信息存到全局状态 store 里，后续 Controller 可以直接取
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            new UserPrincipal(userId, username), null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // 无论 Token 是否合法，都要放行让请求继续往下走
        // 如果 Token 无效，后续的 Controller 检查 authentication 时会返回 401
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer Token
     * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     *              ^^^^^^^ 去掉这 7 个字符，剩下的就是纯 token
     */
    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
