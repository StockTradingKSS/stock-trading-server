package com.KimStock.adapter.out.external.chart;

import com.KimStock.adapter.out.external.error.ClientErrorHandler;
import com.KimStock.adapter.out.external.kiwoom.auth.KiwoomAuthAdapter;
import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class KiwoomMinuteChartClient {
    private static final DateTimeFormatter yyyyMMddHHmm = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private final WebClient webClient;
    private final KiwoomAuthAdapter kiwoomAuthAdapter;
    private final ClientErrorHandler clientErrorHandler;

    public KiwoomMinuteChartClient(
            @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient,
            KiwoomAuthAdapter kiwoomAuthAdapter, ClientErrorHandler clientErrorHandler) {

        this.webClient = kiwoomWebClient;
        this.kiwoomAuthAdapter = kiwoomAuthAdapter;
        this.clientErrorHandler = clientErrorHandler;
    }

    public Mono<List<StockCandle>> loadMinuteCandles(MinuteStockCandleRequest request) {
        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> executeLoadMinuteStockCandleListApiBy(token, request))
                .flatMap(response -> {
                    if (!response.isSuccess()) {
                        log.error("차트 조회 실패: {}", response.returnMsg());
                        return Mono.error(new RuntimeException("차트 조회 실패: " + response.returnMsg()));
                    }

                    log.info("차트 조회 성공");
                    List<MinuteStockCandleResponse.ChartItem> chartItemList = response.chartItems();
                    List<StockCandle> stockCandleList = chartItemList.stream()
                            .map(chartItem -> chartItem.mapToStockCandle(request.stk_cd(), CandleInterval.MINUTE))
                            .toList();

                    LocalDateTime lastDateTime = request.lastDateTime();
                    if (stockCandleList.isEmpty() || lastDateTime == null) {
                        return Mono.just(stockCandleList);
                    }

                    // 최근 날짜 값을 포함한다면 삭제해 준다.
                    boolean hasLastDateTime = stockCandleList.getFirst().getOpenTime().equals(lastDateTime);
                    if (hasLastDateTime) {
                        stockCandleList.removeFirst();
                    }

                    return Mono.just(
                            stockCandleList
                    );
                })
                .onErrorResume(e -> {
                    log.error("[Load Minute Candle Error] : {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<MinuteStockCandleResponse> executeLoadMinuteStockCandleListApiBy(String token, MinuteStockCandleRequest request) {
        String nextKey = "";
        LocalDateTime lastDateTime = request.lastDateTime();
        if (lastDateTime != null) {
            nextKey = "A" + request.stk_cd() + lastDateTime.format(yyyyMMddHHmm) + "0000010000";
        }
        return webClient.post()
                .uri("/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "ka10080")
                .header("cont-yn", "Y")
                .header("next-key", nextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(this::handleMinuteChartResponse);
    }

    private Mono<MinuteStockCandleResponse> handleMinuteChartResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            List<String> hasNext = response.headers().header("cont-yn");
            if (!hasNext.isEmpty()) {
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
        return clientErrorHandler.handleErrorResponse(response, "Minute Stock Candle API call failed. ");
    }

    public record MinuteStockCandleRequest(
            String stk_cd, // 종목 코드
            String tic_scope, //틱범위
            String upd_stkpc_tp, // 수정주가구분
            LocalDateTime lastDateTime // 가장 최근 날짜 값 (응답에서 포함되지 않음)
    ) {
        @Builder
        public MinuteStockCandleRequest {
            stk_cd = Objects.requireNonNullElse(stk_cd, "039490");
        }

        public static MinuteStockCandleRequest of(String stockCode, Integer ticScope, boolean isUpdatedPrice, LocalDateTime lastDateTime) {
            return MinuteStockCandleRequest.builder()
                    .stk_cd(stockCode + "_AL")
                    .tic_scope(String.valueOf(ticScope))
                    .upd_stkpc_tp(String.valueOf(isUpdatedPrice ? 1 : 0))
                    .lastDateTime(lastDateTime)
                    .build();
        }
    }

    /**
     * 주식 차트 응답 DTO
     */
    public record MinuteStockCandleResponse(
            @JsonProperty("stk_cd") String stockCode,
            @JsonProperty("stk_min_pole_chart_qry") List<ChartItem> chartItems,
            @JsonProperty("return_code") Integer returnCode,
            @JsonProperty("return_msg") String returnMsg
    ) {
        @Builder
        public MinuteStockCandleResponse {
            stockCode = Objects.requireNonNullElse(stockCode, "");
            chartItems = Objects.requireNonNullElse(chartItems, new ArrayList<>());
            returnCode = Objects.requireNonNullElse(returnCode, 0);
            returnMsg = Objects.requireNonNullElse(returnMsg, "");
        }

        /**
         * 응답이 정상인지 확인
         *
         * @return 정상 여부
         */
        public boolean isSuccess() {
            return returnCode != null && returnCode == 0;
        }

        /**
         * 차트 데이터 아이템
         */
        public record ChartItem(
                @JsonProperty("cur_prc") String currentPrice,          // 현재가
                @JsonProperty("trde_qty") String tradeQuantity,        // 거래량
                @JsonProperty("cntr_tm") String contractTime,          // 체결시간
                @JsonProperty("open_pric") String openPrice,           // 시가
                @JsonProperty("high_pric") String highPrice,           // 고가
                @JsonProperty("low_pric") String lowPrice,             // 저가
                @JsonProperty("upd_stkpc_tp") String updateStockPriceType,    // 상승하락구분
                @JsonProperty("upd_rt") String updateRate,             // 등락률
                @JsonProperty("bic_inds_tp") String bigIndustryType,   // 대업종구분
                @JsonProperty("sm_inds_tp") String smallIndustryType,  // 소업종구분
                @JsonProperty("stk_infr") String stockInfo,            // 종목정보
                @JsonProperty("upd_stkpc_event") String updateStockPriceEvent, // 상승하락이벤트
                @JsonProperty("pred_close_pric") String predictedClosePrice    // 예상종가
        ) {
            @Builder
            public ChartItem {
                currentPrice = Objects.requireNonNullElse(currentPrice, "");
                tradeQuantity = Objects.requireNonNullElse(tradeQuantity, "");
                contractTime = Objects.requireNonNullElse(contractTime, "");
                openPrice = Objects.requireNonNullElse(openPrice, "");
                highPrice = Objects.requireNonNullElse(highPrice, "");
                lowPrice = Objects.requireNonNullElse(lowPrice, "");
                updateStockPriceType = Objects.requireNonNullElse(updateStockPriceType, "");
                updateRate = Objects.requireNonNullElse(updateRate, "");
                bigIndustryType = Objects.requireNonNullElse(bigIndustryType, "");
                smallIndustryType = Objects.requireNonNullElse(smallIndustryType, "");
                stockInfo = Objects.requireNonNullElse(stockInfo, "");
                updateStockPriceEvent = Objects.requireNonNullElse(updateStockPriceEvent, "");
                predictedClosePrice = Objects.requireNonNullElse(predictedClosePrice, "");
            }

            public StockCandle mapToStockCandle(String stockCode, CandleInterval interval) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                String price = currentPrice.startsWith("-") ? currentPrice.substring(1) : currentPrice;
                String open = openPrice.startsWith("-") ? openPrice.substring(1) : openPrice;
                String high = highPrice.startsWith("-") ? highPrice.substring(1) : highPrice;
                String low = lowPrice.startsWith("-") ? lowPrice.substring(1) : lowPrice;

                return StockCandle.builder()
                        .code(stockCode)
                        .candleInterval(interval)
                        .currentPrice(Long.parseLong(price))
                        .volume(Long.parseLong(tradeQuantity))
                        .openPrice(Long.parseLong(open))
                        .highPrice(Long.parseLong(high))
                        .lowPrice(Long.parseLong(low))
                        .closePrice(Long.parseLong(price))  // 현재가를 종가로 사용
                        .openTime(LocalDateTime.parse(contractTime, formatter))
                        .build();
            }
        }
    }
}
