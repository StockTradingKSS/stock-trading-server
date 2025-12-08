package com.kokimstocktrading.adapter.in.web.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 수정 요청")
public record UpdateUserRequest(
    @Schema(description = "이메일", example = "newemail@example.com")
    String email,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "새 비밀번호 (선택사항)", example = "newpassword123")
    String password
) {

}
