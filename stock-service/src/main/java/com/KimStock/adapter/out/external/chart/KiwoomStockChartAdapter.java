package com.KimStock.adapter.out.external.chart;

import com.KimStock.adapter.out.external.kiwoom.auth.KiwoomAuthAdapter;
import com.KimStock.application.port.out.LoadStockChartPort;
import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import com.common.ExternalSystemAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@ExternalSystemAdapter
@Slf4j
public class KiwoomStockChartAdapter implements LoadStockChartPort {
    private final WebClient webClient;
    private final KiwoomAuthAdapter kiwoomAuthAdapter;

    public KiwoomStockChartAdapter(
            @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient,
            KiwoomAuthAdapter kiwoomAuthAdapter) {

        this.webClient = kiwoomWebClient;
        this.kiwoomAuthAdapter = kiwoomAuthAdapter;
    }

    private static <T> Mono<T> handleErrorResponse(ClientResponse response, String failInfoString) {
        return response.bodyToMono(String.class)
                .flatMap(errorBody -> {
                            String errorMessage = failInfoString + "Status : " + response.statusCode() + ", Body : " + errorBody;
                            return Mono.error(new RuntimeException(errorMessage));
                        }
                );
    }

    @Override
    public Mono<List<StockCandle>> loadStockCandleListBy(String stockCode, CandleInterval candleInterval) {
        switch (candleInterval) {
            case MINUTE -> {
                return executeLoadMinuteStockCandleListBy(MinuteStockCandleRequest.of(stockCode, 1, true));
            }
            case DAY -> {

            }
            case WEEK -> {

            }
            case MONTH -> {

            }
            case YEAR -> {

            }
            default -> {
                throw new IllegalArgumentException("Invalid candle interval : " + candleInterval);
            }
        }
        return Mono.empty();
    }

    private Mono<List<StockCandle>> executeLoadMinuteStockCandleListBy(MinuteStockCandleRequest request) {
        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> executeLoadMinuteStockCandleListApiBy(token, request))
                .flatMap(response -> {
                    if (!response.isSuccess()) {
                        log.error("차트 조회 실패: {}", response.returnMsg());
                        return Mono.error(new RuntimeException("차트 조회 실패: " + response.returnMsg()));
                    }

                    log.info("차트 조회 성공");
                    List<MinuteStockCandleResponse.ChartItem> chartItemList = response.chartItems();
                    return Mono.just(
                            chartItemList.stream()
                                    .map(chartItem -> chartItem.mapToStockCandle(request.stk_cd(), CandleInterval.MINUTE))
                                    .toList()
                    );
                })
                .onErrorResume(e -> {
                    log.error("[Load Minute Candle Error] : {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<MinuteStockCandleResponse> executeLoadMinuteStockCandleListApiBy(String token, MinuteStockCandleRequest request) {
        return webClient.post()
                .uri("/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "ka10080")  // 매수주문 TR코드
                .header("cont-yn", "Y")
                .header("next-key", "A005930_AL2025043020000000010000")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(this::handleMinuteChartResponse);
    }

    private Mono<MinuteStockCandleResponse> handleMinuteChartResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            List<String> hasNext = response.headers().header("cont-yn");
            if(!hasNext.isEmpty()){
                String nextKey = response.headers().header("next-key").getFirst();
                log.info("next key: {}", nextKey);
            }


            return response.bodyToMono(MinuteStockCandleResponse.class)
                    .doOnSuccess(orderResponse -> {
                        if (orderResponse.isSuccess()) {
                            log.info("Minute Stock Candle API call successful size: {}", orderResponse.chartItems().size());
                        } else {
                            log.warn("Minute Stock Candle returned error: {}", orderResponse.returnMsg());
                        }
                    });
        }
        return handleErrorResponse(response, "Minute Stock Candle API call failed. ");
    }
}
