package com.kokimstocktrading.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * User 도메인 모델
 * 헥사고날 아키텍처 패턴을 따르는 불변 도메인 객체
 */
@Builder
@Getter
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class User {
    private Long id;
    private String username;       // 로그인용 고유 식별자
    private String password;       // BCrypt 암호화된 비밀번호
    private String email;
    private String name;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
