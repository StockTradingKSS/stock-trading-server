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

@ExternalSystemAdapter
@RequiredArgsConstructor
@Slf4j
public class TossInvestLoadStockChartAdapter implements LoadStockChartPort {
    
    private final TossInvestChartClient tossInvestChartClient;
    
    @Override
    public Mono<List<StockCandle>> loadStockCandleListBy(String stockCode, CandleInterval candleInterval, LocalDateTime lastDateTime) {
        log.info("Loading {} candles for stock code: {} from: {}", candleInterval, stockCode, lastDateTime);

        // lastDateTime을 from 파라미터로 사용하여 300개 데이터 로드
        return tossInvestChartClient.loadCandles(stockCode, candleInterval, 300, lastDateTime)
                .onErrorResume(error -> {
                    log.error("Failed to load chart data for stock {}: {}", stockCode, error.getMessage());
                    return Mono.just(List.of());
                });
    }
}
