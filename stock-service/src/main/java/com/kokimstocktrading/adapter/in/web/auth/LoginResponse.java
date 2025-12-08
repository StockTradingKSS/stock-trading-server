package com.kokimstocktrading.adapter.in.web.auth;

import com.kokimstocktrading.application.auth.port.in.AuthenticationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "로그인 응답")
public record LoginResponse(
    @Schema(description = "JWT 토큰")
    String token,

    @Schema(description = "사용자 ID")
    UUID userId,

    @Schema(description = "사용자명")
    String username,

    @Schema(description = "역할")
    String role
) {

  public static LoginResponse from(AuthenticationResult result) {
    return new LoginResponse(
        result.token(),
        result.userId(),
        result.username(),
        result.role().name()
    );
  }
}
