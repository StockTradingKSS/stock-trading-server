package com.KimStock.application.port.out;

import com.KimStock.domain.model.Stock;
import com.KimStock.domain.model.type.MarketType;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoadStockListPort {
    Mono<List<Stock>> loadStockInfoListBy(MarketType marketType, String contYn, String nextKey);
}
