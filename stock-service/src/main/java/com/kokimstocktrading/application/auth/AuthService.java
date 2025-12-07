package com.kokimstocktrading.application.auth;

import com.common.UseCase;
import com.kokimstocktrading.application.auth.port.in.*;
import com.kokimstocktrading.application.auth.port.out.DeleteTokenPort;
import com.kokimstocktrading.application.auth.port.out.SaveTokenPort;
import com.kokimstocktrading.application.user.port.out.LoadUserPort;
import com.kokimstocktrading.application.user.port.out.UpdateLastLoginPort;
import com.kokimstocktrading.domain.user.User;
import com.kokimstocktrading.domain.user.UserToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Authentication Service
 * 사용자 인증, JWT 토큰 생성 및 로그아웃 처리
 */
@UseCase
@Slf4j
@RequiredArgsConstructor
public class AuthService implements AuthenticateUseCase, LogoutUseCase {

    private final LoadUserPort loadUserPort;
    private final UpdateLastLoginPort updateLastLoginPort;
    private final SaveTokenPort saveTokenPort;
    private final DeleteTokenPort deleteTokenPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expiration}")
    private long expiration;

    @Override
    public AuthenticationResult authenticate(LoginCommand command) {
        log.info("사용자 인증 시도: {}", command.username());

        User user = loadUserPort.findByUsername(command.username())
            .orElseThrow(() -> new IllegalArgumentException("사용자명 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new IllegalArgumentException("사용자명 또는 비밀번호가 올바르지 않습니다");
        }

        String token = jwtTokenProvider.generateToken(
            user.getId(),
            user.getUsername(),
            user.getRole()
        );

        // 토큰을 DB에 저장하여 세션 관리
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(expiration / 1000);

        UserToken userToken = UserToken.builder()
            .userId(user.getId())
            .token(token)
            .createdAt(now)
            .expiresAt(expiresAt)
            .build();

        saveTokenPort.save(userToken);

        // 마지막 로그인 시간 업데이트
        updateLastLoginPort.updateLastLogin(user.getId(), now);

        log.info("사용자 인증 및 토큰 저장 완료: {}", user.getUsername());

        return new AuthenticationResult(
            token,
            user.getId(),
            user.getUsername(),
            user.getRole()
        );
    }

    @Override
    public void logout(String token) {
        log.info("사용자 로그아웃 - 토큰 무효화");
        deleteTokenPort.deleteByToken(token);
        log.info("토큰 삭제 완료");
    }
}
