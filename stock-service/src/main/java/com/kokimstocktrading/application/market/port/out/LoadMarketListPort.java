package com.kokimstocktrading.application.market.port.out;

import com.kokimstocktrading.domain.market.Market;
import com.kokimstocktrading.domain.market.MarketType;
import java.util.List;
import reactor.core.publisher.Mono;

public interface LoadMarketListPort {

  Mono<List<Market>> loadMarketListBy(MarketType marketType, String contYn, String nextKey);
}
