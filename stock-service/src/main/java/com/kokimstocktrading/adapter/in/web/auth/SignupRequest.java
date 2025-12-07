package com.kokimstocktrading.adapter.in.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 요청")
public record SignupRequest(
    @Schema(description = "사용자명", example = "newuser")
    String username,

    @Schema(description = "비밀번호", example = "password123")
    String password,

    @Schema(description = "이메일", example = "user@example.com")
    String email,

    @Schema(description = "이름", example = "김철수")
    String name
) {}
