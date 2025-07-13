package com.KimStock.adapter.out.external.chart;

import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class TossInvestChartClient {

    private final WebClient tossInvestWebClient;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    // 생성자에서 직접 @Qualifier 사용
    public TossInvestChartClient(@Qualifier("tossInvestWebClient") WebClient tossInvestWebClient) {
        this.tossInvestWebClient = tossInvestWebClient;
    }


    public Mono<List<StockCandle>> loadCandles(String stockCode, CandleInterval interval, int count, LocalDateTime from) {
        String timeFrame = getTimeFrame(interval);

        return tossInvestWebClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, stockCode, timeFrame, count, from))
                .retrieve()
                .bodyToMono(TossInvestChartResponse.class)
                .map(response -> convertToStockCandles(response, interval))
                .doOnSuccess(candles -> log.info("Successfully loaded {} {} candles for stock {} from {}",
                        candles != null ? candles.size() : 0, interval, stockCode, from))
                .doOnError(error -> log.error("Failed to load {} candles for stock {} from {}: {}",
                        interval, stockCode, from, error.getMessage()));
    }

    private URI buildUri(UriBuilder uriBuilder, String stockCode, String timeFrame, int count, LocalDateTime from) {
        // 스톡 코드에서 공백 제거 및 정규화
        String cleanStockCode = stockCode.trim();

        var builder = uriBuilder
                .path("/api/v1/c-chart/kr-s/{stockCode}/{timeFrame}")
                .queryParam("count", count)
                .queryParam("useAdjustedRate", "true");

        if (from != null) {
            // LocalDateTime을 ISO 8601 형식으로 변환 (URL 인코딩됨)
            String fromStr = from.format(ISO_FORMATTER);
            builder.queryParam("from", fromStr);
            log.debug("Adding from parameter: {}", fromStr);
        }

        log.debug("Building URI with stock code: [{}]", cleanStockCode);
        return builder.build("A" + cleanStockCode, timeFrame);
    }

    private String getTimeFrame(CandleInterval interval) {
        return switch (interval) {
            case MINUTE -> "min:1";
            case DAY -> "day:1";
            case WEEK -> "week:1";
            case MONTH -> "month:1";
            case YEAR -> "year:1";
            default -> throw new IllegalArgumentException("Unsupported interval: " + interval);
        };
    }

    private List<StockCandle> convertToStockCandles(TossInvestChartResponse response, CandleInterval interval) {
        if (response == null || response.getResult() == null || response.getResult().getCandles() == null) {
            return List.of();
        }

        String stockCode = response.getResult().getCode();
        List<TossInvestChartResponse.Candle> candles = response.getResult().getCandles();

        return candles.stream()
                .map(candle -> StockCandle.builder()
                        .code(stockCode)
                        .candleInterval(interval)
                        .currentPrice(candle.getClose())
                        .previousPrice(candle.getBase())
                        .volume(candle.getVolume())
                        .openPrice(candle.getOpen())
                        .highPrice(candle.getHigh())
                        .lowPrice(candle.getLow())
                        .closePrice(candle.getClose())
                        .openTime(candle.getDt().toLocalDateTime())  // OffsetDateTime -> LocalDateTime 변환
                        .build())
                .toList();
    }
}
