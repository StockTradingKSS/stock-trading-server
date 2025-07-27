package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.application.candle.port.out.LoadStockCandlePort;
import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.candle.StockCandle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 이동평균선과 맞닿을 가격 계산기
 * 현재 시간의 이평선과 맞닿을 가격을 구하는 식
 * 현재가격 + (1 ~ period -1 까지의 가격 합)  = period * 현재 가격
 * 즉 현재가격 = (1 ~ period -1 까지의 가격 합) / (period - 1)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MovingAverageTouchPriceCalculator {

    private final LoadStockCandlePort loadStockCandlePort;

    public Mono<Long> calculateTouchPrice(String stockCode, int period, CandleInterval interval) {
        return loadStockCandlePort.loadStockCandleListBy(stockCode, interval,
                LocalDateTime.now(), (long) period)
                .<Long>handle((candles, sink) -> {
                    if (candles.size() < period - 1) {
                        log.warn("이평선 터치 가격 계산을 위한 충분한 캔들 데이터가 없습니다. 요구: {}, 실제: {}",
                                period - 1, candles.size());
                        sink.error(new IllegalStateException(
                                String.format("이평선 터치 가격 계산을 위한 데이터 부족: 요구 %d개, 실제 %d개",
                                        period - 1, candles.size())));
                        return;
                    }

                    // 현재를 제외한 1 ~ period-1까지의 가격 합계
                    List<StockCandle> pastCandles = candles.subList(1, period);

                    BigDecimal pastPriceSum = pastCandles.stream()
                            .map(StockCandle::getClosePrice)
                            .map(BigDecimal::valueOf)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 현재가격 = (1 ~ period-1까지의 가격 합) / (period - 1)
                    BigDecimal touchPrice = pastPriceSum.divide(BigDecimal.valueOf(period - 1), 0, RoundingMode.HALF_UP);
                    
                    log.debug("이평선 터치 가격 계산 완료: 종목={}, 기간={}{}, 터치가격={}", 
                            stockCode, period, interval.getDisplayName(), touchPrice);

                    sink.next(touchPrice.longValue());
                })
                .doOnError(error -> log.error("이평선 터치 가격 계산 중 오류 발생: 종목={}, 기간={}{}", 
                        stockCode, period, interval.getDisplayName(), error));
    }
}
