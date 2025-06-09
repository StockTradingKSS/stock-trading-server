package com.KimStock.adapter.out.external.chart;

import com.KimStock.application.port.out.LoadStockChartPort;
import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import com.common.ExternalSystemAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static com.KimStock.adapter.out.external.chart.KiwoomMinuteChartClient.MinuteStockCandleRequest;

@ExternalSystemAdapter
@RequiredArgsConstructor
@Slf4j
public class KiwoomLoadStockChartAdapter implements LoadStockChartPort {
    private final KiwoomMinuteChartClient kiwoomMinuteChartClient;
    private final KiwoomDayChartClient kiwoomDayChartClient;
    private final KiwoomWeekChartClient kiwoomWeekChartClient;
    private final KiwoomMonthChartClient kiwoomMonthChartClient;
    private final KiwoomYearChartClient kiwoomYearChartClient;

    /**
     * @param lastDateTime : lastDateTime 의 이전 날짜 데이터 중 최대 300개를 가져옵니다. ( Kiwoom API 특성 )
     */
    @Override
    public Mono<List<StockCandle>> loadStockCandleListBy(String stockCode, CandleInterval candleInterval, LocalDateTime lastDateTime) {
        switch (candleInterval) {
            case MINUTE -> {
                return kiwoomMinuteChartClient.loadMinuteCandles(MinuteStockCandleRequest.of(stockCode, 1, true, lastDateTime));
            }
            case DAY -> {
                return kiwoomDayChartClient.loadDayCandles(KiwoomDayChartClient.DayStockCandleRequest.of(stockCode, true, lastDateTime));
            }
            case WEEK -> {
                return kiwoomWeekChartClient.loadWeekCandles(KiwoomWeekChartClient.WeekStockCandleRequest.of(stockCode, true, lastDateTime));
            }
            case MONTH -> {
                return kiwoomMonthChartClient.loadMonthCandles(KiwoomMonthChartClient.MonthStockCandleRequest.of(stockCode, true, lastDateTime));
            }
            case YEAR -> {
                return kiwoomYearChartClient.loadYearCandles(KiwoomYearChartClient.YearStockCandleRequest.of(stockCode, true, lastDateTime));
            }
            default -> {
                throw new IllegalArgumentException("Invalid candle interval : " + candleInterval);
            }
        }
//        return Mono.empty();
    }
}
