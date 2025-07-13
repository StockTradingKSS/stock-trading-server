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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.KimStock.adapter.out.external.chart.StockCodeParser.getOriginalStockCode;

@Component
@Slf4j
public class KiwoomYearChartClient {
    private static final DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final WebClient webClient;
    private final KiwoomAuthAdapter kiwoomAuthAdapter;
    private final ClientErrorHandler clientErrorHandler;

    public KiwoomYearChartClient(
            @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient,
            KiwoomAuthAdapter kiwoomAuthAdapter, ClientErrorHandler clientErrorHandler) {

        this.webClient = kiwoomWebClient;
        this.kiwoomAuthAdapter = kiwoomAuthAdapter;
        this.clientErrorHandler = clientErrorHandler;
    }

    /**
     * @return : 키움증권 일봉 조회는 최대 600개의 데이터가 조회 됨
     */
    public Mono<List<StockCandle>> loadYearCandles(YearStockCandleRequest request) {
        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> executeLoadYearStockCandleListApiBy(token, request))
                .flatMap(response -> {
                    if (!response.isSuccess()) {
                        log.error("차트 조회 실패: {}", response.returnMsg());
                        return Mono.error(new RuntimeException("차트 조회 실패: " + response.returnMsg()));
                    }

                    log.info("차트 조회 성공");
                    List<YearStockCandleResponse.YearCandleItem> YearCandleItemList = response.YearCandleItems();
                    List<StockCandle> stockCandleList = new ArrayList<>(YearCandleItemList.stream()
                            .map(YearCandleItem -> YearCandleItem.mapToStockCandle(getOriginalStockCode(request.stk_cd())))
                            .toList());


                    String baseDateTimeStr = request.base_dt();
                    if (stockCandleList.isEmpty() || baseDateTimeStr == null) {
                        return Mono.just(stockCandleList);
                    }

                    // 최근 날짜 값을 포함한다면 삭제해 준다.
                    LocalDateTime openDateTime = stockCandleList.getFirst().getOpenTime();
                    boolean hasBaseDateTime = openDateTime.format(yyyyMMdd).equals(baseDateTimeStr);
                    if (hasBaseDateTime) {
                        stockCandleList.removeFirst();
                    }

                    return Mono.just(
                            stockCandleList
                    );
                })
                .onErrorResume(e -> {
                    log.error("[Load Year Candle Error] : {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<YearStockCandleResponse> executeLoadYearStockCandleListApiBy(String token, YearStockCandleRequest request) {
        String nextKey = "";
        return webClient.post()
                .uri("/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "ka10094")
                .header("cont-yn", "Y")
                .header("next-key", nextKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(this::handleYearChartResponse);
    }

    private Mono<YearStockCandleResponse> handleYearChartResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            List<String> hasNext = response.headers().header("cont-yn");
            if (!hasNext.isEmpty()) {
                String nextKey = response.headers().header("next-key").getFirst();
                log.info("next key: {}", nextKey);
            }

            return response.bodyToMono(YearStockCandleResponse.class)
                    .doOnSuccess(orderResponse -> {
                        if (orderResponse.isSuccess()) {
                            log.info("Year Stock Candle API call successful size: {}", orderResponse.YearCandleItems().size());
                        } else {
                            log.warn("Year Stock Candle returned error: {}", orderResponse.returnMsg());
                        }
                    });
        }
        return clientErrorHandler.handleErrorResponse(response, "Year Stock Candle API call failed. ");
    }

    public record YearStockCandleRequest(
            String stk_cd, // 종목 코드
            String base_dt, // 기준일자 YYYYMMDD
            String upd_stkpc_tp // 수정주가구분
    ) {
        @Builder
        public YearStockCandleRequest {
            stk_cd = Objects.requireNonNullElse(stk_cd, "039490");
        }

        public static YearStockCandleRequest of(String stockCode, boolean isUpdatedPrice, LocalDateTime lastDateTime) {
            if (lastDateTime == null) {
                lastDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
            }
            return YearStockCandleRequest.builder()
                    .stk_cd(stockCode + "_AL")
                    .upd_stkpc_tp(String.valueOf(isUpdatedPrice ? 1 : 0))
                    .base_dt(lastDateTime != null ? lastDateTime.format(yyyyMMdd) : null)
                    .build();
        }
    }

    /**
     * 주식 차트 응답 DTO
     */
    public record YearStockCandleResponse(
            @JsonProperty("stk_cd") String stockCode,
            @JsonProperty("stk_yr_pole_chart_qry") List<YearCandleItem> YearCandleItems,
            @JsonProperty("return_code") Integer returnCode,
            @JsonProperty("return_msg") String returnMsg
    ) {
        @Builder
        public YearStockCandleResponse {
            stockCode = Objects.requireNonNullElse(stockCode, "");
            YearCandleItems = Objects.requireNonNullElse(YearCandleItems, new ArrayList<>());
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
        public record YearCandleItem(
                @JsonProperty("cur_prc") String currentPrice,          // 현재가
                @JsonProperty("trde_qty") String tradeQuantity,        // 거래량
                @JsonProperty("trde_prica") String tradeAmount,        // 거래대금
                @JsonProperty("dt") String date,                       // 일자
                @JsonProperty("open_pric") String openPrice,           // 시가
                @JsonProperty("high_pric") String highPrice,           // 고가
                @JsonProperty("low_pric") String lowPrice,             // 저가
                @JsonProperty("upd_stkpc_tp") String updateStockPriceType,    // 수정주가구분
                @JsonProperty("upd_rt") String updateRate,             // 수정비율
                @JsonProperty("bic_inds_tp") String bigIndustryType,   // 대업종구분
                @JsonProperty("sm_inds_tp") String smallIndustryType,  // 소업종구분
                @JsonProperty("stk_infr") String stockInfo,            // 종목정보
                @JsonProperty("upd_stkpc_event") String updateStockPriceEvent, // 수정주가이벤트
                @JsonProperty("pred_close_pric") String previousClosePrice     // 전일종가
        ) {
            public YearCandleItem {
                currentPrice = Objects.requireNonNullElse(currentPrice, "");
                tradeQuantity = Objects.requireNonNullElse(tradeQuantity, "");
                tradeAmount = Objects.requireNonNullElse(tradeAmount, "");
                date = Objects.requireNonNullElse(date, "");
                openPrice = Objects.requireNonNullElse(openPrice, "");
                highPrice = Objects.requireNonNullElse(highPrice, "");
                lowPrice = Objects.requireNonNullElse(lowPrice, "");
                updateStockPriceType = Objects.requireNonNullElse(updateStockPriceType, "");
                updateRate = Objects.requireNonNullElse(updateRate, "");
                bigIndustryType = Objects.requireNonNullElse(bigIndustryType, "");
                smallIndustryType = Objects.requireNonNullElse(smallIndustryType, "");
                stockInfo = Objects.requireNonNullElse(stockInfo, "");
                updateStockPriceEvent = Objects.requireNonNullElse(updateStockPriceEvent, "");
                previousClosePrice = Objects.requireNonNullElse(previousClosePrice, "0");
                if (previousClosePrice.isBlank()) {
                    previousClosePrice = "0";
                }
            }

            public StockCandle mapToStockCandle(String stockCode) {
//                log.info(this.toString());
                LocalDate localDate = LocalDate.parse(date, yyyyMMdd);
                LocalDateTime dateTime = localDate.atStartOfDay(); // 00:00:00으로 설정
                String price = currentPrice.startsWith("-") ? currentPrice.substring(1) : currentPrice;
                String open = openPrice.startsWith("-") ? openPrice.substring(1) : openPrice;
                String high = highPrice.startsWith("-") ? highPrice.substring(1) : highPrice;
                String low = lowPrice.startsWith("-") ? lowPrice.substring(1) : lowPrice;

                return StockCandle.builder()
                        .code(stockCode)
                        .candleInterval(CandleInterval.YEAR)
                        .currentPrice(Long.parseLong(price))
                        .previousPrice(Long.parseLong(previousClosePrice))
                        .volume(Long.parseLong(tradeQuantity))
                        .openPrice(Long.parseLong(open))
                        .highPrice(Long.parseLong(high))
                        .lowPrice(Long.parseLong(low))
                        .closePrice(Long.parseLong(price))  // 현재가를 종가로 사용
                        .openTime(dateTime)
                        .build();
            }
        }

    }
}
