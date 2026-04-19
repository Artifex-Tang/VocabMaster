package com.vocabmaster.security;

import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-ttl-seconds:7200}")
    private long accessTtlSeconds;

    @Value("${app.jwt.refresh-ttl-seconds:604800}")
    private long refreshTtlSeconds;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret 长度不足 32 字节，请检查配置");
        }
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String uuid, Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(uuid)
                .claim("uid", userId)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTtlSeconds * 1000L))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String uuid) {
        Date now = new Date();
        return Jwts.builder()
                .subject(uuid)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTtlSeconds * 1000L))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token，校验签名和过期时间。
     * @throws BizException TOKEN_EXPIRED 或 UNAUTHORIZED
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    /** 返回 accessToken 剩余有效秒数（用于设置 Redis 黑名单 TTL） */
    public long remainingTtlSeconds(Claims claims) {
        long expMs = claims.getExpiration().getTime();
        long remainMs = expMs - System.currentTimeMillis();
        return Math.max(remainMs / 1000, 0);
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }
}
