package com.kokimstocktrading.application.monitoring.calculator;

import com.kokimstocktrading.application.candle.port.out.LoadStockCandlePort;
import com.kokimstocktrading.domain.candle.CandleInterval;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 추세선과 맞닿을 가격 계산기 현재 시간의 추세선과 맞닿을 가격을 구하는 식 즉 현재가격 = 추세선 시작 날짜 + 현재날짜와 시작날짜 사이의 봉 개수 * 기울기
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TrendLineTouchPriceCalculator {

  private final LoadStockCandlePort loadStockCandlePort;

  public Mono<Long> calculateTargetPrice(String stockCode, LocalDateTime toDate, BigDecimal slope,
      CandleInterval interval) {
    return loadStockCandlePort.loadStockCandleListBy(stockCode, interval,
            LocalDateTime.now(), toDate)
        .<Long>handle((stockCandleList, sink) -> {
          if (stockCandleList.isEmpty()) {
            log.warn("추세선 터치 가격 계산을 위한 충분한 캔들 데이터가 없습니다. 요구: {}, 실제: {}",
                2, 0);
            sink.error(new IllegalStateException("추세선 터치 가격 계산을 위한 데이터 부족"));
            return;
          }

          // 봉 개수 계산 (현재부터 시작점까지)
          int candleCount = stockCandleList.size() - 1;

          // 추세선 시작점 가격 (가장 과거 봉의 종가)
          Long startPrice = stockCandleList.getFirst().getClosePrice();

          // 현재 시점의 추세선 가격 = 시작가격 + (봉개수 * 기울기)
          BigDecimal currentTrendLinePrice = BigDecimal.valueOf(startPrice)
              .add(slope.multiply(BigDecimal.valueOf(candleCount)));

          sink.next(currentTrendLinePrice.setScale(0, RoundingMode.HALF_UP).longValue());
        })
        .doOnError(error -> log.error("추세선 터치 가격 계산 중 오류 발생: 종목={}, 기간={}",
            stockCode, interval.getDisplayName(), error));
  }
}
