package com.kokimstocktrading.application.stock.port.out;

import com.kokimstocktrading.domain.stock.Stock;
import com.kokimstocktrading.domain.market.MarketType;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoadStockListPort {
    Mono<List<Stock>> loadStockInfoListBy(MarketType marketType, String contYn, String nextKey);
}
