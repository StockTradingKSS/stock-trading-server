package com.kokimstocktrading.application.auth;

import com.kokimstocktrading.domain.user.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token Provider
 * JWT 토큰 생성 및 검증 처리
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 인증된 사용자를 위한 JWT 토큰 생성
     */
    public String generateToken(UUID userId, String username, Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
            .subject(username)
            .claim("userId", userId.toString())
            .claim("role", role.name())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact();
    }

    /**
     * JWT 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * JWT 토큰에서 클레임 추출
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * JWT 토큰에서 사용자명 추출
     */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public UUID getUserId(String token) {
        String userIdStr = getClaims(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    /**
     * JWT 토큰에서 역할 추출
     */
    public Role getRole(String token) {
        String roleName = getClaims(token).get("role", String.class);
        return Role.valueOf(roleName);
    }
}
