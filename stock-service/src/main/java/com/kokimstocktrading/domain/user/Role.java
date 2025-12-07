package com.kokimstocktrading.domain.user;

/**
 * 사용자 권한 열거형
 * - USER: 기본 접근 권한을 가진 일반 사용자
 * - TRADER: 거래 권한
 * - ADMIN: 전체 관리자 권한
 */
public enum Role {
    USER,
    TRADER,
    ADMIN
}
