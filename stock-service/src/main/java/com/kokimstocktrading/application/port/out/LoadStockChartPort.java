package com.kokimstocktrading.application.port.out;

import com.kokimstocktrading.domain.model.StockCandle;
import com.kokimstocktrading.domain.model.type.CandleInterval;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface LoadStockChartPort {
    Mono<List<StockCandle>> loadStockCandleListBy(
            String stockCode,
            CandleInterval candleInterval,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime);
}
