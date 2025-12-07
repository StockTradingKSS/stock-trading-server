package com.common;

import java.lang.annotation.*;

/**
 * 메서드 레벨 보안을 위한 커스텀 인가 어노테이션
 *
 * 사용법:
 * - @Authorize: 인증만 필요 (역할 확인 없음)
 * - @Authorize(roles = {"ADMIN"}): 인증 및 ADMIN 역할 필요
 * - @Authorize(roles = {"USER", "TRADER"}): 인증 및 USER 또는 TRADER 역할 필요
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Authorize {
    /**
     * 허용된 역할. 비어있으면 인증만 필요 (역할 확인 없음).
     * @return 허용된 역할 이름 배열
     */
    String[] roles() default {};
}
