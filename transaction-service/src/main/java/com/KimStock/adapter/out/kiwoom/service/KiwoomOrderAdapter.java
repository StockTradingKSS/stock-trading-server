package com.KimStock.adapter.out.kiwoom.service;

import com.KimStock.adapter.out.kiwoom.dto.*;
import com.KimStock.application.port.out.RequestStockOrderPort;
import com.KimStock.domain.model.OrderResult;
import com.KimStock.domain.model.type.TradeType;
import com.common.ExternalSystemAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExternalSystemAdapter
@Slf4j
public class KiwoomOrderAdapter implements RequestStockOrderPort {
    private final WebClient webClient;
    private final KiwoomAuthAdapter kiwoomAuthAdapter;

    public KiwoomOrderAdapter(
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
    public Mono<OrderResult> requestBuyStock(String stockCode, int quantity, double price, TradeType tradeType) {
        // OrderType을 String 코드로 변환
        String typeCode = tradeType != null ? tradeType.getCode() : TradeType.LIMIT.getCode();
        OrderRequest request = OrderRequest.createBuyOrder(stockCode, quantity, price, typeCode);
        
        log.info("매수 주문 요청 정보: stockCode={}, quantity={}, price={}, tradeType={}",
                stockCode, quantity, price, tradeType);
        
        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> executeBuyOrderApi(token, request))
                .flatMap(response -> {
                    if (!response.isSuccess()) {
                        log.error("매수 주문 실패: {}", response.returnMessage());
                        return Mono.error(new RuntimeException("매수 주문 실패: " + response.returnMessage()));
                    }
                    
                    log.info("매수 주문 성공: orderNo={}, message={}", response.orderNo(), response.returnMessage());
                    return Mono.just(
                        OrderResult.createBuyResult(
                                response.orderNo(),
                                stockCode,
                                response.marketType(),
                                quantity,
                                response.returnMessage()
                        )
                    );
                })
                .onErrorResume(e -> {
                    log.error("[Buy Stock Order Error] : {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<OrderResponse> executeBuyOrderApi(String token, OrderRequest request) {
        return webClient.post()
                .uri("/api/dostk/ordr")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "kt10000")  // 매수주문 TR코드
                .header("cont-yn", "N")
                .header("next-key", "")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(this::handleOrderResponse);
    }

    @Override
    public Mono<OrderResult> requestSellStock(String stockCode, int quantity, double price, TradeType tradeType) {
        // OrderType을 String 코드로 변환
        String typeCode = tradeType != null ? tradeType.getCode() : TradeType.LIMIT.getCode();
        OrderRequest request = OrderRequest.createSellOrder(stockCode, quantity, price, typeCode);
        
        log.info("매도 주문 요청 정보: stockCode={}, quantity={}, price={}, tradeType={}",
                stockCode, quantity, price, tradeType);
        
        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> executeSellOrderApi(token, request))
                .flatMap(response -> {
                    if (!response.isSuccess()) {
                        log.error("매도 주문 실패: {}", response.returnMessage());
                        return Mono.error(new RuntimeException("매도 주문 실패: " + response.returnMessage()));
                    }
                    
                    log.info("매도 주문 성공: orderNo={}, message={}", response.orderNo(), response.returnMessage());
                    return Mono.just(
                        OrderResult.createSellResult(
                                response.orderNo(),
                                stockCode,
                                response.marketType(),
                                quantity,
                                response.returnMessage()
                        )
                    );
                })
                .onErrorResume(e -> {
                    log.error("[Sell Stock Order Error] : {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<OrderResponse> executeSellOrderApi(String token, OrderRequest request) {
        return webClient.post()
                .uri("/api/dostk/ordr")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "kt10001")  // 매도주문 TR코드
                .header("cont-yn", "N")
                .header("next-key", "")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(this::handleOrderResponse);
    }

    @Override
    public Mono<OrderResult> requestModifyOrder(String orderNo, String stockCode, int quantity, double price) {
        ModifyOrderRequest request = ModifyOrderRequest.of(orderNo, stockCode, quantity, price);
        
        log.info("정정 주문 요청 정보: orderNo={}, stockCode={}, quantity={}, price={}", 
                orderNo, stockCode, quantity, price);
        
        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> executeModifyOrderApi(token, request))
                .flatMap(response -> {
                    if (!response.isSuccess()) {
                        log.error("정정 주문 실패: {}", response.returnMessage());
                        return Mono.error(new RuntimeException("정정 주문 실패: " + response.returnMessage()));
                    }
                    
                    int modifiedQty = response.modifyQuantity() != null ? 
                            Integer.parseInt(response.modifyQuantity().trim()) : quantity;
                    
                    log.info("정정 주문 성공: orderNo={}, baseOrderNo={}, message={}", 
                            response.orderNo(), response.baseOriginalOrderNo(), response.returnMessage());
                            
                    return Mono.just(
                        OrderResult.createModifyResult(
                                response.orderNo(),
                                response.baseOriginalOrderNo(),
                                stockCode,
                                response.marketType(),
                                modifiedQty,
                                response.returnMessage()
                        )
                    );
                })
                .onErrorResume(e -> {
                    log.error("[Modify Order Error] : {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<OrderResponse> executeModifyOrderApi(String token, ModifyOrderRequest request) {
        return webClient.post()
                .uri("/api/dostk/ordr")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "kt10002")  // 정정주문 TR코드
                .header("cont-yn", "N")
                .header("next-key", "")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(this::handleOrderResponse);
    }

    @Override
    public Mono<OrderResult> requestCancelOrder(String orderNo, String stockCode) {
        CancelOrderRequest request = CancelOrderRequest.createFullCancel(orderNo, stockCode);
        
        return executeCancelOrder(request, stockCode, 0);
    }

    @Override
    public Mono<OrderResult> requestCancelOrder(String orderNo, String stockCode, int quantity) {
        CancelOrderRequest request = CancelOrderRequest.createPartialCancel(orderNo, stockCode, quantity);
        
        return executeCancelOrder(request, stockCode, quantity);
    }
    
    private Mono<OrderResult> executeCancelOrder(CancelOrderRequest request, String stockCode, int quantity) {
        log.info("취소 주문 요청 정보: orderNo={}, stockCode={}, quantity={}", 
                request.originalOrderNo(), stockCode, quantity);
                
        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> executeCancelOrderApi(token, request))
                .flatMap(response -> {
                    if (!response.isSuccess()) {
                        log.error("취소 주문 실패: {}", response.returnMessage());
                        return Mono.error(new RuntimeException("취소 주문 실패: " + response.returnMessage()));
                    }
                    
                    int canceledQty = response.cancelQuantity() != null ? 
                            Integer.parseInt(response.cancelQuantity().trim()) : quantity;
                    
                    log.info("취소 주문 성공: orderNo={}, baseOrderNo={}, message={}", 
                            response.orderNo(), response.baseOriginalOrderNo(), response.returnMessage());
                            
                    return Mono.just(
                        OrderResult.createCancelResult(
                                response.orderNo(),
                                response.baseOriginalOrderNo(),
                                stockCode,
                                canceledQty,
                                response.returnMessage()
                        )
                    );
                })
                .onErrorResume(e -> {
                    log.error("[Cancel Order Error] : {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<OrderResponse> executeCancelOrderApi(String token, CancelOrderRequest request) {
        return webClient.post()
                .uri("/api/dostk/ordr")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "kt10003")  // 취소주문 TR코드
                .header("cont-yn", "N")
                .header("next-key", "")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(this::handleOrderResponse);
    }

    private Mono<OrderResponse> handleOrderResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(OrderResponse.class)
                    .doOnSuccess(orderResponse -> {
                        if (orderResponse.isSuccess()) {
                            log.info("Order API call successful: {}", orderResponse);
                        } else {
                            log.warn("Order API call returned error: {}", orderResponse.returnMessage());
                        }
                    });
        }
        return handleErrorResponse(response, "Order request failed. ");
    }
}
