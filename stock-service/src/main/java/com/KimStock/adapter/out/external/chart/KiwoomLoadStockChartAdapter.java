package com.KimStock.adapter.out.external.chart;

import com.KimStock.application.port.out.LoadStockChartPort;
import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import com.common.ExternalSystemAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

//@ExternalSystemAdapter
@RequiredArgsConstructor
@Slf4j
public class KiwoomLoadStockChartAdapter implements LoadStockChartPort {
    
    // Toss API로 전환
    private final TossInvestChartClient tossInvestChartClient;

    /**
     * @param lastDateTime : lastDateTime 의 이전 날짜 데이터를 가져옵니다.
     */
    @Override
    public Mono<List<StockCandle>> loadStockCandleListBy(String stockCode, CandleInterval candleInterval, LocalDateTime lastDateTime) {
        String cleanStockCode = stockCode.trim();
        log.info("Loading {} candles for stock code: [{}] from: {} using Toss API", candleInterval, cleanStockCode, lastDateTime);
        
        // Toss API로 데이터 로드, lastDateTime을 from 파라미터로 사용
        return tossInvestChartClient.loadCandles(cleanStockCode, candleInterval, 300, lastDateTime)
                .onErrorResume(error -> {
                    log.error("Failed to load chart data for stock {}: {}", cleanStockCode, error.getMessage());
                    return Mono.just(List.of());
                });
    }
}
