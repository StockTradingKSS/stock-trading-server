package com.kokimstocktrading.adapter.out.external.stock;

import com.kokimstocktrading.adapter.out.external.error.ClientErrorHandler;
import com.kokimstocktrading.adapter.out.external.kiwoom.auth.KiwoomAuthAdapter;
import com.kokimstocktrading.application.port.out.LoadStockListPort;
import com.kokimstocktrading.domain.model.Stock;
import com.kokimstocktrading.domain.model.type.MarketType;
import com.common.ExternalSystemAdapter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ExternalSystemAdapter
@Slf4j
public class KiwoomLoadStockListAdapter implements LoadStockListPort {
    private final WebClient webClient;
    private final KiwoomAuthAdapter kiwoomAuthAdapter;
    private final ClientErrorHandler clientErrorHandler;

    public KiwoomLoadStockListAdapter(
            @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient,
            KiwoomAuthAdapter kiwoomAuthAdapter, ClientErrorHandler clientErrorHandler) {

        this.webClient = kiwoomWebClient;
        this.kiwoomAuthAdapter = kiwoomAuthAdapter;
        this.clientErrorHandler = clientErrorHandler;
    }

    /**
     * 연속 조회가 지원되는 종목 정보 리스트 조회
     *
     * @param marketType 시장 종류
     * @param contYn     연속조회여부 (Y/N)
     * @param nextKey    연속조회키
     * @return 종목 정보 응답
     */
    public Mono<List<Stock>> loadStockInfoListBy(MarketType marketType, String contYn, String nextKey) {
        StockInfoRequest stockInfoRequest = StockInfoRequest.of(marketType);

        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> loadStockInfoListApi(token, stockInfoRequest, contYn, nextKey))
                .flatMap(stockItems ->
                        Mono.just(stockItems.stream()
                                .map(StockInfoResponse.StockItem::mapToStock)
                                .toList())
                )
                .onErrorResume(e -> {
                    log.error("[Load StockInfoList Error] : {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<List<StockInfoResponse.StockItem>> loadStockInfoListApi(String token, StockInfoRequest stockInfoRequest, String contYn, String nextKey) {
        return webClient.post()
                .uri("/api/dostk/stkinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "ka10099")                         // TR명 지정
                .header("cont-yn", contYn != null ? contYn : "N")    // 연속조회여부
                .header("next-key", nextKey != null ? nextKey : "")  // 연속조회키
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(stockInfoRequest)
                .exchangeToMono(
                        this::handleStockInfoResponse
                );
    }

    private Mono<List<StockInfoResponse.StockItem>> handleStockInfoResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(StockInfoResponse.class)
                    .flatMap(stockResponse -> Mono.just(stockResponse.stockItems()));
        }
        return clientErrorHandler.handleErrorResponse(response, "Stock info request failed.");
    }

    /**
     * 종목 정보 요청 DTO
     */
    private record StockInfoRequest(
            String mrkt_tp     // 시장구분(0:전체, 1:코스피, 2:코스닥)
    ) {
        @Builder
        public StockInfoRequest {
            mrkt_tp = Objects.requireNonNullElse(mrkt_tp, "0");
        }

        public static StockInfoRequest of(MarketType marketType) {
            // 0:코스피,10:코스닥,3:ELW,8:ETF,30:K-OTC,50:코넥스,5:신주인수권,4:뮤추얼펀드,6:리츠,9:하이일드
            String mrkt_tp = "0";

            switch (marketType) {
                case KOSPI -> mrkt_tp = "0";
                case KOSDAQ -> mrkt_tp = "10";
                case null, default -> throw new IllegalArgumentException("marketType is null");
            }

            return StockInfoRequest.builder()
                    .mrkt_tp(mrkt_tp)
                    .build();
        }
    }

    /**
     * 종목 정보 응답 DTO
     */
    private record StockInfoResponse(
            @JsonProperty("return_msg") String returnMsg,
            @JsonProperty("return_code") Integer returnCode,
            @JsonProperty("list") List<StockItem> stockItems
    ) {
        @Builder
        public StockInfoResponse {
            returnMsg = Objects.requireNonNullElse(returnMsg, "");
            returnCode = Objects.requireNonNullElse(returnCode, 0);
            stockItems = Objects.requireNonNullElse(stockItems, new ArrayList<>());
        }

        /**
         * 종목 정보 아이템
         */
        private record StockItem(
                String code,                           // 종목코드
                String name,                           // 종목명
                @JsonProperty("listCount") String listCount,    // 상장주식수
                @JsonProperty("auditInfo") String auditInfo,    // 감사정보
                @JsonProperty("regDay") String regDay,          // 등록일
                @JsonProperty("lastPrice") String lastPrice,    // 현재가
                String state,                          // 종목 상태
                @JsonProperty("marketCode") String marketCode,  // 시장코드
                @JsonProperty("marketName") String marketName,  // 시장명
                @JsonProperty("upName") String upName,          // 업종명
                @JsonProperty("upSizeName") String upSizeName,  // 규모명
                @JsonProperty("companyClassName") String companyClassName, // 기업형태
                @JsonProperty("orderWarning") String orderWarning,        // 주문경고
                @JsonProperty("nxtEnable") String nxtEnable              // 다음페이지유무
        ) {
            @Builder
            public StockItem {
                code = Objects.requireNonNullElse(code, "");
                name = Objects.requireNonNullElse(name, "");
                listCount = Objects.requireNonNullElse(listCount, "");
                auditInfo = Objects.requireNonNullElse(auditInfo, "");
                regDay = Objects.requireNonNullElse(regDay, "");
                lastPrice = Objects.requireNonNullElse(lastPrice, "");
                state = Objects.requireNonNullElse(state, "");
                marketCode = Objects.requireNonNullElse(marketCode, "");
                marketName = Objects.requireNonNullElse(marketName, "");
                upName = Objects.requireNonNullElse(upName, "");
                upSizeName = Objects.requireNonNullElse(upSizeName, "");
                companyClassName = Objects.requireNonNullElse(companyClassName, "");
                orderWarning = Objects.requireNonNullElse(orderWarning, "");
                nxtEnable = Objects.requireNonNullElse(nxtEnable, "");
            }

            public Stock mapToStock() {
                return Stock.builder()
                        .code(code)
                        .name(name)
                        .listCount(Long.valueOf(listCount))
                        .auditInfo(auditInfo)
                        .regDay(regDay)
                        .state(state)
                        .marketCode(marketCode)
                        .marketName(marketName)
                        .upName(upName)
                        .upSizeName(upSizeName)
                        .companyClassName(companyClassName)
                        .orderWarning(orderWarning)
                        .nxtEnable("Y".equals(nxtEnable))
                        .build();
            }
        }
    }
}
