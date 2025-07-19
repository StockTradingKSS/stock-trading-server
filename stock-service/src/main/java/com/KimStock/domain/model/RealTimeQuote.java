package com.KimStock.domain.model;

import lombok.Builder;

@Builder
public record RealTimeQuote(
        String type,
        String name,
        String item,
        String currentPrice,      // 현재가
        String priceChange,       // 전일대비
        String changeRate,        // 등락율
        String askPrice,          // 최우선 매도호가
        String bidPrice,          // 최우선 매수호가
        String tradingVolume,     // 거래량
        String accumulatedVolume, // 누적거래량
        String accumulatedAmount, // 누적거래대금
        String openPrice,         // 시가
        String highPrice,         // 고가
        String lowPrice,          // 저가
        String tradeTime          // 체결시간
) {
    // 값이 정상인지 확인하는 유틸리티 메서드
    public boolean hasValidPrices() {
        return currentPrice != null;
    }
}
