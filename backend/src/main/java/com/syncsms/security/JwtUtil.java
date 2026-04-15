package com.syncsms.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @description JWT Token 工具类，支持用户 Token 和设备 Token 两种类型
 */
@Component
public class JwtUtil {

    @Value("${syncsms.jwt.secret}")
    private String secret;

    @Value("${syncsms.jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @description 生成用户 JWT Token
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色
     * @return JWT Token 字符串
     */
    public String generateUserToken(Long userId, String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .claim("type", "user")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * @description 生成设备 JWT Token（长期有效）
     * @param deviceId  设备ID
     * @param deviceKey 设备密钥
     * @return JWT Token 字符串
     */
    public String generateDeviceToken(Long deviceId, String deviceKey) {
        return Jwts.builder()
                .subject(deviceKey)
                .claim("deviceId", deviceId)
                .claim("type", "device")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration * 30))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * @description 解析 Token，返回 Claims
     * @param token JWT Token 字符串
     * @return Claims 对象，解析失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }
}
