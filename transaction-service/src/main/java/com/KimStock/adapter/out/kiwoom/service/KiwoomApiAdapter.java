package com.KimStock.adapter.out.kiwoom.service;

import com.KimStock.adapter.out.kiwoom.dto.MarketCodeRequest;
import com.KimStock.adapter.out.kiwoom.dto.MarketCodeResponse;
import com.KimStock.adapter.out.kiwoom.dto.StockInfoRequest;
import com.KimStock.adapter.out.kiwoom.dto.StockInfoResponse;
import com.KimStock.application.port.out.LoadMarketListPort;
import com.KimStock.application.port.out.LoadStockListPort;
import com.KimStock.domain.model.Market;
import com.KimStock.domain.model.Stock;
import com.KimStock.domain.model.type.MarketType;
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
public class KiwoomApiAdapter implements LoadStockListPort, LoadMarketListPort {
    private final WebClient webClient;
    private final KiwoomAuthAdapter kiwoomAuthAdapter;

    public KiwoomApiAdapter(
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
        return handleErrorResponse(response, "Stock info request failed.");
    }

    /**
     * 종목 정보 리스트 조회
     *
     * @param marketType 시장 유형
     * @return 종목 정보 응답
     */
    public Mono<List<Market>> loadMarketListBy(MarketType marketType, String contYn, String nextKey) {
        MarketCodeRequest marketCodeRequest = MarketCodeRequest.of(marketType);
        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> loadMarketCodeListApi(token, marketCodeRequest, contYn, nextKey))
                .flatMap(marketCodeList ->
                        Mono.just(marketCodeList.stream()
                                .map(MarketCodeResponse.MarketCode::mapToMarket)
                                .toList())
                )
                .onErrorResume(e -> {
                    log.error("[Load Market List Error] : {}", e.getMessage());
                    return Mono.error(e);
                });

    }

    private Mono<List<MarketCodeResponse.MarketCode>> loadMarketCodeListApi(String token, MarketCodeRequest marketCodeRequest, String contYn, String nextKey) {

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/dostk/stkinfo")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "ka10101")
                .header("cont-yn", contYn != null ? contYn : "N")    // 연속조회여부
                .header("next-key", nextKey != null ? nextKey : "")  // 연속조회키
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(marketCodeRequest)
                .exchangeToMono(this::handleMarketCodeResponse);
    }

    private Mono<List<MarketCodeResponse.MarketCode>> handleMarketCodeResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(MarketCodeResponse.class)
                    .flatMap(marketCodeResponse -> Mono.just(marketCodeResponse.marketCodeList()));
        }
        return handleErrorResponse(response, "Market code request failed.");
    }
}
