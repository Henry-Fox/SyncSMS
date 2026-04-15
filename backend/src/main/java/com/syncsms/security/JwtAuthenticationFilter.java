package com.syncsms.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * @description JWT 认证过滤器，从请求头解析 Token 并设置 SecurityContext
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 避免在 user/login 等公开接口上写入 Authentication
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            if (claims != null) {
                String type = claims.get("type", String.class);
                UsernamePasswordAuthenticationToken auth;

                if ("device".equals(type)) {
                    Long deviceId = claims.get("deviceId", Long.class);
                    auth = new UsernamePasswordAuthenticationToken(
                            deviceId, null,
                            List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
                } else {
                    String role = claims.get("role", String.class);
                    Long userId = claims.get("userId", Long.class);
                    auth = new UsernamePasswordAuthenticationToken(
                            userId, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                }
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
