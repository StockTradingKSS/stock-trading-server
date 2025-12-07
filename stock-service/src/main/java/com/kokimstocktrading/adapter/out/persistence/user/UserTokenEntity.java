package com.kokimstocktrading.adapter.out.persistence.user;

import com.kokimstocktrading.domain.user.UserToken;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserToken JPA 엔티티
 * 세션 관리를 위한 활성 JWT 토큰 저장
 */
@Entity
@Table(name = "user_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 정적 팩토리 메서드: 도메인 -> 엔티티
     */
    public static UserTokenEntity from(UserToken userToken) {
        return UserTokenEntity.builder()
            .id(userToken.getId())
            .userId(userToken.getUserId())
            .token(userToken.getToken())
            .createdAt(userToken.getCreatedAt())
            .expiresAt(userToken.getExpiresAt())
            .build();
    }

    /**
     * 도메인 변환: 엔티티 -> 도메인
     */
    public UserToken toDomain() {
        return UserToken.builder()
            .id(this.id)
            .userId(this.userId)
            .token(this.token)
            .createdAt(this.createdAt)
            .expiresAt(this.expiresAt)
            .build();
    }
}
