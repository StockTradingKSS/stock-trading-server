package com.kokimstocktrading.adapter.out.external.market;

import com.kokimstocktrading.application.condition.port.out.TradingTimePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * 거래 시간 확인 어댑터
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TradingTimeAdapter implements TradingTimePort {

    private final KrxMarketStatusAdapter krxMarketStatusAdapter;

    // 한국 주식시장 운영 시간
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 0);    // 09:00
    private static final LocalTime MARKET_CLOSE_TIME = LocalTime.of(15, 30);  // 15:30
    private static final LocalTime MARKET_OPEN_START = LocalTime.of(7, 50);   // 07:50
    private static final LocalTime MARKET_OPEN_END = LocalTime.of(8, 0);      // 08:00
    private static final LocalTime MARKET_CLOSE_START = LocalTime.of(20, 10); // 20:10
    private static final LocalTime MARKET_CLOSE_END = LocalTime.of(20, 20);   // 20:20

    @Override
    public boolean isTradingTime() {
        LocalTime now = LocalTime.now();
        
        // 1. 시간대 체크 (09:00 ~ 15:30)
        boolean isWithinTradingHours = now.isAfter(MARKET_OPEN_TIME.minusSeconds(1)) && 
                                      now.isBefore(MARKET_CLOSE_TIME.plusSeconds(1));
        
        if (!isWithinTradingHours) {
            return false;
        }

        // 2. 휴장일 체크 (동기적으로 캐시된 값 사용 또는 간단한 체크)
        try {
            return Boolean.TRUE.equals(krxMarketStatusAdapter.loadTodayMarketStatus()
                    .map(status -> {
                        boolean isOpen = status.isOpen();
                        log.debug("오늘 시장 상태: {}", isOpen ? "개장" : "휴장");
                        return isOpen;
                    })
                    .block()); // 동기 처리 (실제로는 캐싱 권장)
        } catch (Exception e) {
            log.warn("시장 상태 조회 실패, 시간 기반으로 판단", e);
            return true; // 오류 시 기본값: 거래 가능
        }
    }

    @Override
    public boolean isMarketOpenTime() {
        LocalTime now = LocalTime.now();
        return now.isAfter(MARKET_OPEN_START.minusSeconds(1)) && 
               now.isBefore(MARKET_OPEN_END.plusSeconds(1));
    }

    @Override
    public boolean isMarketCloseTime() {
        LocalTime now = LocalTime.now();
        return now.isAfter(MARKET_CLOSE_START.minusSeconds(1)) && 
               now.isBefore(MARKET_CLOSE_END.plusSeconds(1));
    }
}
