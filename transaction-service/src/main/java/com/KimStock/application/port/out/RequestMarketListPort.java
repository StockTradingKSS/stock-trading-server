package com.KimStock.application.port.out;

import com.KimStock.domain.model.Market;
import com.KimStock.domain.model.type.MarketType;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RequestMarketListPort {
    Mono<List<Market>> getMarketListByMarketCode(MarketType marketType);
}
