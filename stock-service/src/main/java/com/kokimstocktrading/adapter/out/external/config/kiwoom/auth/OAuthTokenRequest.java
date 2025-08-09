package com.kokimstocktrading.adapter.out.external.config.kiwoom.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.Objects;

/**
 * OAuth 토큰 요청 DTO
 */
public record
OAuthTokenRequest(
        @JsonProperty("grant_type") String grantType,  // 인증 타입 (client_credentials)
        String appkey,                                // 앱 키
        String secretkey                              // 앱 시크릿
) {
    @Builder
    public OAuthTokenRequest {
        grantType = Objects.requireNonNullElse(grantType, "client_credentials");
        appkey = Objects.requireNonNullElse(appkey, "");
        secretkey = Objects.requireNonNullElse(secretkey, "");
    }
}
