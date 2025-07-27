package com.kokimstocktrading.domain.realtime;

import lombok.Builder;

/**
 *  주식 시장에서 "quote"는 주로 주식의 현재 가격 정보를 의미합니다.
 *  "주식 quote"는 특정 주식의 현재 시세, 매수/매도 호가, 거래량 등 투자 관련 정보를 포괄적으로 나타내는 용어로 사용됩니다.
 */

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
