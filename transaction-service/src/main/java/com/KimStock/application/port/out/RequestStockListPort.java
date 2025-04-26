package com.KimStock.application.port.out;

import com.KimStock.domain.model.Stock;
import com.KimStock.domain.model.type.MarketType;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RequestStockListPort {
    Mono<List<Stock>> getStockListByMarketCode(MarketType marketType);
}
