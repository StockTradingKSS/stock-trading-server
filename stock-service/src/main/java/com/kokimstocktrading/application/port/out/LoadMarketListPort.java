package com.kokimstocktrading.application.port.out;

import com.kokimstocktrading.domain.model.Market;
import com.kokimstocktrading.domain.model.type.MarketType;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoadMarketListPort {
    Mono<List<Market>> loadMarketListBy(MarketType marketType, String contYn, String nextKey);
}
