package com.KimStock.application.port.out;

import com.KimStock.domain.model.Market;
import com.KimStock.domain.model.type.MarketType;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoadMarketListPort {
    Mono<List<Market>> loadMarketListBy(MarketType marketType, String contYn, String nextKey);
}
