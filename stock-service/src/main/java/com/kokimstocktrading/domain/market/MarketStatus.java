package com.kokimstocktrading.domain.market;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 시장 운영 상태 도메인
 */
@Getter
@Builder
public class MarketStatus {

    private final LocalDate date;
    private final boolean isOpen;
    private final String reason; // 휴장 사유 (공휴일명 등)

    /**
     * 시장 개장일 생성
     */
    public static MarketStatus open(LocalDate date) {
        return MarketStatus.builder()
                .date(date)
                .isOpen(true)
                .build();
    }

    /**
     * 시장 휴장일 생성
     */
    public static MarketStatus closed(LocalDate date, String reason) {
        return MarketStatus.builder()
                .date(date)
                .isOpen(false)
                .reason(reason)
                .build();
    }
}
