package com.KimStock.application.port.out;

import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoadStockChartPort {
    Mono<List<StockCandle>> loadStockCandleListBy(String stockCode, CandleInterval candleInterval);
}
