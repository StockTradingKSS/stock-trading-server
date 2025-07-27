package com.kokimstocktrading.application.candle.port.out;

import com.kokimstocktrading.domain.candle.StockCandle;
import com.kokimstocktrading.domain.candle.CandleInterval;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface LoadStockCandlePort {
    Mono<List<StockCandle>> loadStockCandleListBy(
            String stockCode,
            CandleInterval candleInterval,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime);

    Mono<Long> loadStockCandleListBy(
            String stockCode,
            CandleInterval candleInterval,
            LocalDateTime fromDateTime,
            Long count);
}
