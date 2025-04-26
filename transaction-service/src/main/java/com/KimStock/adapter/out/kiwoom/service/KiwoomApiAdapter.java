package com.KimStock.adapter.out.kiwoom.service;

import com.KimStock.adapter.out.kiwoom.dto.MarketCodeRequest;
import com.KimStock.adapter.out.kiwoom.dto.MarketCodeResponse;
import com.KimStock.adapter.out.kiwoom.dto.StockInfoRequest;
import com.KimStock.adapter.out.kiwoom.dto.StockInfoResponse;
import com.KimStock.application.port.out.RequestMarketListPort;
import com.KimStock.application.port.out.RequestStockListPort;
import com.KimStock.domain.model.Market;
import com.KimStock.domain.model.Stock;
import com.KimStock.domain.model.type.MarketType;
import com.common.ExternalSystemAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;


@ExternalSystemAdapter
public class KiwoomApiAdapter implements RequestStockListPort, RequestMarketListPort {
    private final Logger log = LoggerFactory.getLogger(KiwoomApiAdapter.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final KiwoomAuthAdapter authService;
    private final KiwoomAuthAdapter kiwoomAuthAdapter;


    public KiwoomApiAdapter(
            @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient,
            @Value("${kiwoom.api.base-url:https://api.kiwoom.com}") String baseUrl, KiwoomAuthAdapter authService,
            KiwoomAuthAdapter kiwoomAuthAdapter) {

        this.authService = authService;
        this.objectMapper = new ObjectMapper();
        this.webClient = kiwoomWebClient;

        log.info("KiwoomApiAdapter initialized with base URL: {}", baseUrl);
        this.kiwoomAuthAdapter = kiwoomAuthAdapter;
    }


    /**
     * 종목 정보 리스트 조회
     *
     * @param marketType 시장 유형
     * @return 종목 정보 응답
     */
    public Mono<List<Stock>> getStockListByMarketCode(MarketType marketType) {
        // 기본 조회는 연속 조회 없이 진행
        return getStockInfoListWithContinue(marketType, null, null)
                .flatMap(stockItems ->
                        Mono.just(stockItems.stream()
                                .map(StockInfoResponse.StockItem::mapToStock)
                                .toList())
                );
    }

    /**
     * 종목 정보 리스트 조회
     *
     * @param marketType 시장 유형
     * @return 종목 정보 응답
     */
    public Mono<List<Market>> getMarketListByMarketCode(MarketType marketType) {
        // 기본 조회는 연속 조회 없이 진행
        return getMarketCodeList(marketType, null, null)
                .flatMap(marketCodeList ->
                        Mono.just(marketCodeList.stream()
                                .map(MarketCodeResponse.MarketCode::mapToMarket)
                                .toList())
                );
    }


    /**
     * 연속 조회가 지원되는 종목 정보 리스트 조회
     *
     * @param marketType 시장 종류
     * @param contYn     연속조회여부 (Y/N)
     * @param nextKey    연속조회키
     * @return 종목 정보 응답
     */
    private Mono<List<StockInfoResponse.StockItem>> getStockInfoListWithContinue(MarketType marketType, String contYn, String nextKey) {
        StockInfoRequest stockInfoRequest = StockInfoRequest.of(marketType);
        Mono<String> validToken = authService.getValidToken();

        return validToken
                .flatMap(token -> webClient.post()
                        .uri("/api/dostk/stkinfo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("api-id", "ka10099")                         // TR명 지정
                        .header("cont-yn", contYn != null ? contYn : "N")    // 연속조회여부
                        .header("next-key", nextKey != null ? nextKey : "")  // 연속조회키
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(stockInfoRequest)
                        .exchangeToMono(
                                response -> {
                                    log.info("Response status: {}", response.statusCode());

                                    // 응답 헤더 정보 로깅 (연속조회 관련)
                                    response.headers().asHttpHeaders().forEach((key, value) -> {
                                        if (key.equals("cont-yn") || key.equals("next-key") || key.equals("api-id")) {
                                            log.info("Header - {}: {}", key, value);
                                        }
                                    });

                                    if (response.statusCode().is2xxSuccessful()) {
                                        // 응답 본문을 먼저 로깅한 후 StockInfoResponse로 변환
                                        return response.bodyToMono(String.class)
                                                .doOnNext(responseBody -> log.info("Response body: {}", responseBody))
                                                .flatMap(responseBody -> {
                                                    try {
                                                        // ObjectMapper를 사용하여 응답을 StockInfoResponse로 변환
                                                        StockInfoResponse stockResponse = objectMapper.readValue(responseBody, StockInfoResponse.class);
                                                        log.info("Retrieved {} stock items",
                                                                stockResponse.stockItems() != null ?
                                                                        stockResponse.stockItems().size() : 0);
                                                        return Mono.just(stockResponse.stockItems());
                                                    } catch (JsonProcessingException e) {
                                                        log.error("Failed to parse response: {}", e.getMessage());
                                                        return Mono.error(new RuntimeException("Failed to parse response", e));
                                                    }
                                                });
                                    }
                                    return response.bodyToMono(String.class)
                                            .flatMap(errorBody -> {
                                                        log.error("Stock info request failed. Status: {}, Body: {}",
                                                                response.statusCode(), errorBody);
                                                        return Mono.error(
                                                                new RuntimeException("Failed to get stock info. Status: " +
                                                                        response.statusCode() + ", Body: " + errorBody));
                                                    }
                                            );
                                }
                        )
                )
                .onErrorResume(e -> {
                    log.error("Error retrieving stock info marketCodeList: {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    /**
     * 업종코드 리스트 조회
     *
     * @param marketType 시장 종류
     * @return 업종코드 응답
     */
    private Mono<List<MarketCodeResponse.MarketCode>> getMarketCodeList(MarketType marketType, String contYn, String nextKey) {
        MarketCodeRequest marketCodeRequest = MarketCodeRequest.of(marketType);
        Mono<String> validToken = kiwoomAuthAdapter.getValidToken();
        return validToken
                .flatMap(token -> webClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/dostk/stkinfo")
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("api-id", "ka10101")
                        .header("cont-yn", contYn != null ? contYn : "N")    // 연속조회여부
                        .header("next-key", nextKey != null ? nextKey : "")  // 연속조회키
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(marketCodeRequest)
                        .exchangeToMono(response -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                return response.bodyToMono(MarketCodeResponse.class)
                                        .doOnSuccess(marketResponse ->
                                                log.info("Retrieved {} market codes",
                                                        marketResponse.marketCodeList() != null ?
                                                                marketResponse.marketCodeList().size() : 0)
                                        )
                                        .flatMap(
                                                marketCodeResponse -> Mono.just(marketCodeResponse.marketCodeList())
                                        )
                                        ;
                            } else {
                                return response.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.error("Market code request failed. Status: {}, Body: {}",
                                                    response.statusCode(), errorBody);
                                            return Mono.error(
                                                    new RuntimeException("Failed to get market codes. Status: " +
                                                            response.statusCode() + ", Body: " + errorBody));
                                        });
                            }
                        }))
                .onErrorResume(e -> {
                    log.error("Error retrieving market code marketCodeList: {}", e.getMessage());
                    return Mono.error(e);
                });
    }
}
