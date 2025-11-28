package com.kokimstocktrading.application.candle.port.out;

import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.candle.StockCandle;
import java.time.LocalDateTime;
import java.util.List;
import reactor.core.publisher.Mono;

public interface LoadStockCandlePort {

  Mono<List<StockCandle>> loadStockCandleListBy(
      String stockCode,
      CandleInterval candleInterval,
      LocalDateTime fromDateTime,
      LocalDateTime toDateTime);

  Mono<List<StockCandle>> loadStockCandleListBy(
      String stockCode,
      CandleInterval candleInterval,
      LocalDateTime fromDateTime,
      Long count);
}
