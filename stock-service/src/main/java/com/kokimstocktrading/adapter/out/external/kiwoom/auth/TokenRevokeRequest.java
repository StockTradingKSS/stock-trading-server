package com.kokimstocktrading.adapter.out.external.kiwoom.auth;

import lombok.Builder;

import java.util.Objects;

/**
 * 토큰 폐기 요청 DTO
 */
public record TokenRevokeRequest(
        String appkey,         // 앱 키
        String secretkey,      // 앱 시크릿
        String token           // 폐기할 토큰
) {
    @Builder
    public TokenRevokeRequest {
        appkey = Objects.requireNonNullElse(appkey, "");
        secretkey = Objects.requireNonNullElse(secretkey, "");
        token = Objects.requireNonNullElse(token, "");
    }
}
