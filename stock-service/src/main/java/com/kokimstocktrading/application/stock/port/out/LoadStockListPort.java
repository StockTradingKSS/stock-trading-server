package com.kokimstocktrading.application.stock.port.out;

import com.kokimstocktrading.domain.market.MarketType;
import com.kokimstocktrading.domain.stock.Stock;
import java.util.List;
import reactor.core.publisher.Mono;

public interface LoadStockListPort {

  Mono<List<Stock>> loadStockInfoListBy(MarketType marketType, String contYn, String nextKey);
}
