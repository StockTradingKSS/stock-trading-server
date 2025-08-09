package com.kokimstocktrading.application.market.port.out;

import com.kokimstocktrading.domain.market.MarketStatus;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 시장 상태 조회 포트
 */
public interface LoadMarketStatusPort {

    /**
     * 특정 날짜의 시장 운영 상태 조회
     */
    Mono<MarketStatus> loadMarketStatus(LocalDate date);

    /**
     * 오늘 시장 운영 상태 조회
     */
    Mono<MarketStatus> loadTodayMarketStatus();
}
