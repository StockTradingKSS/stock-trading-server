package com.kokimstocktrading.application.port.out;

import com.kokimstocktrading.domain.model.Stock;
import com.kokimstocktrading.domain.model.type.MarketType;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoadStockListPort {
    Mono<List<Stock>> loadStockInfoListBy(MarketType marketType, String contYn, String nextKey);
}
