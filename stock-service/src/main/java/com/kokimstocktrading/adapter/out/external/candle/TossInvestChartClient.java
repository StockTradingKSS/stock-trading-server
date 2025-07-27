package com.kokimstocktrading.adapter.out.external.candle;

import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.candle.StockCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TossInvestChartClient {

    private static final int MAX_COUNT_PER_REQUEST = 300;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'+09:00'");
    private final WebClient tossInvestWebClient;

    public TossInvestChartClient(@Qualifier("tossInvestWebClient") WebClient tossInvestWebClient) {
        this.tossInvestWebClient = tossInvestWebClient;
    }

    public Mono<List<StockCandle>> loadCandles(String stockCode, CandleInterval interval, LocalDateTime from, LocalDateTime to) {
        log.info("Loading {} candles for stock {} from {} to {}", interval, stockCode, from, to);

        return loadCandlesRecursively(stockCode, interval, from, to, new ArrayList<>())
                .doOnSuccess(candles -> log.info("Successfully loaded {} {} candles for stock {} from {} to {}",
                        candles.size(), interval, stockCode, from, to))
                .doOnError(error -> log.error("Failed to load {} candles for stock {} from {} to {}: {}",
                        interval, stockCode, from, to, error.getMessage()));
    }

    private Mono<List<StockCandle>> loadCandlesRecursively(String stockCode, CandleInterval interval,
                                                           LocalDateTime from, LocalDateTime to,
                                                           List<StockCandle> accumulatedCandles) {
        String timeFrame = getTimeFrame(interval);

        return tossInvestWebClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, stockCode, timeFrame, from))
                .retrieve()
                .bodyToMono(TossInvestChartResponse.class)
                .flatMap(response -> {
                            if (response == null || response.getResult() == null || response.getResult().getCandles() == null) {
                                return Mono.just(accumulatedCandles);
                            }

                            List<StockCandle> currentCandles = convertToStockCandles(response, interval);

                            // from(최신)부터 to(과거)까지의 데이터 필터링
                            List<StockCandle> filteredCandles = currentCandles.stream()
                                    .filter(candle -> {
                                        LocalDateTime candleTime = candle.getOpenTime();
                                        return (candleTime.isAfter(to) || candleTime.isEqual(to)) &&
                                                (candleTime.isBefore(from) || candleTime.isEqual(from));
                                    })
                                    .toList();

                            accumulatedCandles.addAll(filteredCandles);

                            log.debug("Loaded {} candles, filtered to {} candles (total accumulated: {})",
                                    currentCandles.size(), filteredCandles.size(), accumulatedCandles.size());

                            // 다음 조회 날짜 확인
                            LocalDateTime nextDateTime = response.getResult().getNextDateTime() != null
                                    ? response.getResult().getNextDateTime().atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime()
                                    : null;

                            // 종료 조건: nextDateTime이 null이거나 to 날짜보다 이전
                            if (nextDateTime == null || nextDateTime.isBefore(to)) {
                                log.info("Reached end of data or target date. Total candles loaded: {}", accumulatedCandles.size());
                                return Mono.just(accumulatedCandles);
                            }

                            // 현재 배치의 마지막 캔들이 to 날짜보다 이전이면 종료
                            if (!currentCandles.isEmpty()) {
                                LocalDateTime lastCandleTime = currentCandles.get(currentCandles.size() - 1).getOpenTime();
                                if (lastCandleTime.isBefore(to) || lastCandleTime.isEqual(to)) {
                                    log.info("Reached target date. Total candles loaded: {}", accumulatedCandles.size());
                                    return Mono.just(accumulatedCandles);
                                }
                            }

                            // 재귀 호출 - nextDateTime부터 계속 조회
                            log.debug("Continuing with nextDateTime: {}", nextDateTime);
                            return loadCandlesRecursively(stockCode, interval, nextDateTime, to, accumulatedCandles);
                        }
                );
    }

    public Mono<Object> loadCandles(String stockCode, CandleInterval interval, LocalDateTime from, Long count) {

        return loadCandlesRecursively(stockCode, interval, from, count, new ArrayList<>())
                .doOnSuccess(candles -> log.info("Successfully loaded {} {} candles for stock {} from {} count {}",
                        candles.size(), interval, stockCode, from, count))
                .doOnError(error -> log.error("Failed to load {} candles for stock {} from {} count {}: {}",
                        interval, stockCode, from, count, error.getMessage()));
    }

    private Mono<List<StockCandle>> loadCandlesRecursively(String stockCode, CandleInterval interval,
                                                           LocalDateTime from, Long count,
                                                           List<StockCandle> accumulatedCandles) {
        String timeFrame = getTimeFrame(interval);

        return tossInvestWebClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, stockCode, timeFrame, from))
                .retrieve()
                .bodyToMono(TossInvestChartResponse.class)
                .flatMap(response -> {
                            if (response == null || response.getResult() == null || response.getResult().getCandles() == null) {
                                return Mono.just(accumulatedCandles);
                            }

                            List<StockCandle> currentCandles = convertToStockCandles(response, interval);

                            // from(최신)부터 과거 방향으로 필터링 (from 이전 데이터만)
                            List<StockCandle> filteredCandles = currentCandles.stream()
                                    .filter(candle -> {
                                        LocalDateTime candleTime = candle.getOpenTime();
                                        return candleTime.isBefore(from) || candleTime.isEqual(from);
                                    })
                                    .toList();

                            accumulatedCandles.addAll(filteredCandles);

                            log.debug("Loaded {} candles, filtered to {} candles (total accumulated: {})",
                                    currentCandles.size(), filteredCandles.size(), accumulatedCandles.size());

                            // 목표 개수에 도달했는지 확인
                            if (accumulatedCandles.size() >= count) {
                                // 필요한 개수만큼만 자르기 (최신 데이터부터 count개)
                                List<StockCandle> result = accumulatedCandles.stream()
                                        .limit(count)
                                        .toList();
                                log.info("Target count reached. Total candles loaded: {}", result.size());
                                return Mono.just(result);
                            }

                            // 다음 조회 날짜 확인
                            LocalDateTime nextDateTime = response.getResult().getNextDateTime() != null
                                    ? response.getResult().getNextDateTime().atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime()
                                    : null;

                            // 종료 조건: nextDateTime이 null
                            if (nextDateTime == null) {
                                log.info("Reached end of data. Total candles loaded: {}", accumulatedCandles.size());
                                return Mono.just(accumulatedCandles);
                            }

                            // 재귀 호출 - nextDateTime부터 계속 조회
                            log.debug("Continuing with nextDateTime: {}, need {} more candles",
                                    nextDateTime, count - accumulatedCandles.size());
                            return loadCandlesRecursively(stockCode, interval, nextDateTime, count, accumulatedCandles);
                        }
                );
    }

    private URI buildUri(UriBuilder uriBuilder, String stockCode, String timeFrame, LocalDateTime from) {
        String cleanStockCode = stockCode.trim();
        String baseUrl = uriBuilder.build().toString();

        StringBuilder url = new StringBuilder(baseUrl);
        url.append("/api/v1/c-chart/kr-s/A").append(cleanStockCode).append("/").append(timeFrame);
        url.append("?count=").append(MAX_COUNT_PER_REQUEST);
        url.append("&useAdjustedRate=true");

        if (from != null) {
            String fromStr = from.format(ISO_FORMATTER);
            String encodedFromStr = fromStr.replace("+", "%2B").replace(":", "%3A");
            url.append("&from=").append(encodedFromStr);
            log.debug("Adding from parameter: {} -> {}", fromStr, encodedFromStr);
        }

        log.debug("Final URL: {}", url);
        return URI.create(url.toString());
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
                        .openTime(candle.getDt().atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime())
                        .build())
                .toList();
    }


}
