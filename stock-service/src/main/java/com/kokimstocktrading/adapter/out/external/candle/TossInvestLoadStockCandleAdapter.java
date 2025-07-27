package com.kokimstocktrading.adapter.out.external.candle;

import com.kokimstocktrading.application.candle.port.out.LoadStockCandlePort;
import com.kokimstocktrading.domain.candle.StockCandle;
import com.kokimstocktrading.domain.candle.CandleInterval;
import com.common.ExternalSystemAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@ExternalSystemAdapter
@RequiredArgsConstructor
@Slf4j
public class TossInvestLoadStockCandleAdapter implements LoadStockCandlePort {

    private final TossInvestChartClient tossInvestChartClient;

    @Override
    public Mono<List<StockCandle>> loadStockCandleListBy(String stockCode, CandleInterval candleInterval, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        return tossInvestChartClient.loadCandles(stockCode, candleInterval, fromDateTime, toDateTime)
                .onErrorResume(error -> {
                    log.error("Failed to load chart data for stock {}: {}", stockCode, error.getMessage());
                    return Mono.just(List.of());
                });
    }
}
