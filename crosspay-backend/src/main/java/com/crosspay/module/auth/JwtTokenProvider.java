package com.crosspay.module.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌工具
 *
 * 支付系统中 JWT 的职责：
 * 1. 无状态认证 — 支付 API 通常高并发，不能用 Session
 * 2. 携带商户身份 — Merchant ID 放在 token 里，后续所有 API 都靠它鉴权
 * 3. 短期有效 — 支付涉及资金，token 有效期不能太长（本项目 24h 只是 demo）
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 生成商户 Token
     * 关键字段：subject = merchantId，claims 里放角色
     */
    public String generateMerchantToken(Long merchantId, String merchantNo, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(merchantId))
                .claim("merchantNo", merchantNo)
                .claim("email", email)
                .claim("role", "MERCHANT")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 生成管理员 Token
     */
    public String generateAdminToken(Long adminId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 从 Token 中提取 Claims
     * 返回 null 表示 token 无效（过期、签名不对、格式错误）
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? Long.valueOf(claims.getSubject()) : null;
    }

    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("role", String.class) : null;
    }
}
