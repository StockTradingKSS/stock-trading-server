package com.kokimstocktrading.application.auth;

/**
 * 인증 컨텍스트를 위한 Thread-local 홀더
 * 요청 기간 동안 인증된 사용자 정보를 저장하는 데 사용
 */
public class AuthContextHolder {

    private static final ThreadLocal<AuthContext> contextHolder = new ThreadLocal<>();

    public static void setContext(AuthContext context) {
        contextHolder.set(context);
    }

    public static AuthContext getContext() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }
}
