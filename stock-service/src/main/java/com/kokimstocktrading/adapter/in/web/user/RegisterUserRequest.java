package com.kokimstocktrading.adapter.in.web.user;

import com.kokimstocktrading.domain.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 등록 요청")
public record RegisterUserRequest(
    @Schema(description = "사용자명", example = "trader1")
    String username,

    @Schema(description = "비밀번호", example = "password123")
    String password,

    @Schema(description = "이메일", example = "trader@example.com")
    String email,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "역할", example = "TRADER")
    Role role
) {

}
