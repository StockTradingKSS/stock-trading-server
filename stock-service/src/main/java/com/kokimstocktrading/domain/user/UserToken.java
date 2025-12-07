package com.kokimstocktrading.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserToken 도메인 모델
 * 세션 관리를 위한 활성 JWT 토큰을 나타냄
 */
@Builder
@Getter
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class UserToken {
    private UUID id;
    private UUID userId;
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
