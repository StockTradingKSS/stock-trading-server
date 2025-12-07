package com.kokimstocktrading.adapter.in.web.user;

import com.kokimstocktrading.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 응답")
public record UserResponse(
    @Schema(description = "사용자 ID")
    Long id,

    @Schema(description = "사용자명")
    String username,

    @Schema(description = "이메일")
    String email,

    @Schema(description = "이름")
    String name,

    @Schema(description = "역할")
    String role,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "마지막 로그인")
    LocalDateTime lastLogin
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getName(),
            user.getRole().name(),
            user.getCreatedAt(),
            user.getLastLogin()
        );
    }
}
