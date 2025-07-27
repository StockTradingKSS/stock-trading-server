package com.kokimstocktrading.adapter.out.external.kiwoom.auth;

import lombok.Builder;

import java.util.Objects;

/**
 * OAuth 토큰 응답 DTO
 */
public record OAuthTokenResponse(
        String expires_dt,        // 만료 일시 (yyyyMMddHHmmss 형식)
        String token_type,        // 토큰 타입 (bearer)
        String token,             // 액세스 토큰
        Integer return_code,          // 응답 코드 (0: 정상)
        String return_msg         // 응답 메시지
) {
    @Builder
    public OAuthTokenResponse {
        expires_dt = Objects.requireNonNullElse(expires_dt, "");
        token_type = Objects.requireNonNullElse(token_type, "bearer");
        token = Objects.requireNonNullElse(token, "");
        return_code = Objects.requireNonNullElse(return_code, -1);
        return_msg = Objects.requireNonNullElse(return_msg, "");
    }
}
