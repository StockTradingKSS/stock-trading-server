package com.kokimstocktrading.application.auth;

import com.kokimstocktrading.domain.user.Role;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증 컨텍스트 인증된 사용자 정보 포함
 */
@Getter
@AllArgsConstructor
public class AuthContext {

  private UUID userId;
  private String username;
  private Role role;
}
