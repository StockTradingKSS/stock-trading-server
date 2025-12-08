package com.kokimstocktrading.adapter.in.web.auth;

import com.common.Authorize;
import com.common.WebAdapter;
import com.kokimstocktrading.adapter.in.web.user.UserResponse;
import com.kokimstocktrading.application.auth.port.in.AuthenticateUseCase;
import com.kokimstocktrading.application.auth.port.in.LoginCommand;
import com.kokimstocktrading.application.auth.port.in.LogoutUseCase;
import com.kokimstocktrading.application.user.port.in.RegisterUserCommand;
import com.kokimstocktrading.application.user.port.in.RegisterUserUseCase;
import com.kokimstocktrading.domain.user.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller 회원가입, 인증, 로그아웃 및 JWT 토큰 관리
 */
@WebAdapter
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "사용자 인증 API")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AuthenticateUseCase authenticateUseCase;
  private final LogoutUseCase logoutUseCase;
  private final RegisterUserUseCase registerUserUseCase;

  @PostMapping("/login")
  @Operation(summary = "로그인", description = "사용자 인증 및 JWT 토큰 발급")
  public LoginResponse login(@RequestBody LoginRequest request) {
    log.info("로그인 시도 - 사용자명: {}", request.username());

    LoginCommand command = new LoginCommand(request.username(), request.password());

    return LoginResponse.from(authenticateUseCase.authenticate(command));
  }

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "회원가입", description = "새로운 사용자 계정 생성 (자동으로 USER 권한 부여)")
  public UserResponse signup(@RequestBody SignupRequest request) {
    log.info("회원가입 요청 - 사용자명: {}", request.username());

    // 회원가입은 자동으로 USER 권한 부여
    RegisterUserCommand command = new RegisterUserCommand(
        request.username(),
        request.password(),
        request.email(),
        request.name(),
        Role.USER
    );

    return UserResponse.from(registerUserUseCase.registerUser(command));
  }

  @PostMapping("/logout")
  @Authorize
  @SecurityRequirement(name = "bearer-token")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "로그아웃", description = "현재 세션 로그아웃 (토큰 무효화)")
  public void logout(@RequestHeader("Authorization") String authHeader) {
    log.info("로그아웃 요청");

    // "Bearer {token}" 형식에서 토큰 추출
    String token = authHeader.substring(7);

    logoutUseCase.logout(token);
  }
}
