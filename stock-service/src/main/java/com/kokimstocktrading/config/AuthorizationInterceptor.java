package com.kokimstocktrading.config;

import com.common.Authorize;
import com.kokimstocktrading.application.auth.AuthContext;
import com.kokimstocktrading.application.auth.AuthContextHolder;
import com.kokimstocktrading.application.auth.JwtTokenProvider;
import com.kokimstocktrading.application.auth.port.out.LoadTokenPort;
import com.kokimstocktrading.domain.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Authorization Interceptor DB에서 JWT 토큰 검증 및 @Authorize 어노테이션 기반 인가 처리
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthorizationInterceptor implements HandlerInterceptor {

  private final JwtTokenProvider jwtTokenProvider;
  private final LoadTokenPort loadTokenPort;
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // 핸들러가 메서드인 경우만 처리 (리소스는 제외)
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    Authorize authorize = handlerMethod.getMethodAnnotation(Authorize.class);

    // @Authorize 어노테이션이 없으면 접근 허용
    if (authorize == null) {
      return true;
    }

    try {
      // Authorization 헤더에서 JWT 토큰 추출
      String token = extractTokenFromRequest(request);

      if (token == null || !jwtTokenProvider.validateToken(token)) {
        log.warn("유효하지 않거나 누락된 JWT 토큰");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"유효하지 않거나 누락된 토큰\"}");
        return false;
      }

      // DB에서 토큰 존재 여부 확인 (세션 검증)
      if (!loadTokenPort.existsByToken(token)) {
        log.warn("DB에서 토큰을 찾을 수 없거나 만료됨");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter()
            .write("{\"error\":\"Unauthorized\",\"message\":\"토큰을 찾을 수 없거나 만료되었습니다\"}");
        return false;
      }

      // 토큰에서 사용자 정보 추출
      UUID userId = jwtTokenProvider.getUserId(token);
      String username = jwtTokenProvider.getUsername(token);
      Role userRole = jwtTokenProvider.getRole(token);

      // Thread-local 컨텍스트에 저장
      AuthContext context = new AuthContext(userId, username, userRole);
      AuthContextHolder.setContext(context);

      // 역할 기반 인가 확인 (역할이 지정된 경우)
      String[] requiredRoles = authorize.roles();
      if (requiredRoles.length > 0) {
        boolean hasRequiredRole = Arrays.stream(requiredRoles)
            .anyMatch(role -> role.equals(userRole.name()));

        if (!hasRequiredRole) {
          log.warn("사용자 {}에게 필요한 권한이 없음. 필요: {}, 보유: {}",
              username, Arrays.toString(requiredRoles), userRole);
          response.setStatus(HttpStatus.FORBIDDEN.value());
          response.setContentType("application/json");
          response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"권한이 부족합니다\"}");
          return false;
        }
      }

      log.debug("사용자 {} 인증 완료, 역할: {}", username, userRole);
      return true;

    } catch (Exception e) {
      log.error("인가 처리 중 오류 발생", e);
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      return false;
    }
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    // Thread-local 컨텍스트 정리 (메모리 누수 방지)
    AuthContextHolder.clear();
  }

  private String extractTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }

    return null;
  }
}
