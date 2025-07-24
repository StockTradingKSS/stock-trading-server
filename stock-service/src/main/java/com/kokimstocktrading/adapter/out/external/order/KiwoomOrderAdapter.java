package com.kokimstocktrading.adapter.out.external.order;

import com.kokimstocktrading.adapter.out.external.error.ClientErrorHandler;
import com.kokimstocktrading.adapter.out.external.kiwoom.auth.KiwoomAuthAdapter;
import com.kokimstocktrading.application.port.out.RequestStockOrderPort;
import com.kokimstocktrading.domain.model.OrderResult;
import com.kokimstocktrading.domain.model.type.TradeType;
import com.common.ExternalSystemAdapter;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private final ClientErrorHandler clientErrorHandler;

    public KiwoomOrderAdapter(
            @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient,
            KiwoomAuthAdapter kiwoomAuthAdapter,
            ClientErrorHandler clientErrorHandler) {

        this.webClient = kiwoomWebClient;
        this.kiwoomAuthAdapter = kiwoomAuthAdapter;
        this.clientErrorHandler = clientErrorHandler;
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
        return clientErrorHandler.handleErrorResponse(response, "Order request failed. ");
    }

    public record ModifyOrderRequest(
            @JsonProperty("dmst_stex_tp") String marketType,      // 국내거래소구분 (KRX, NXT, SOR)
            @JsonProperty("orig_ord_no") String originalOrderNo,  // 원주문번호
            @JsonProperty("stk_cd") String stockCode,             // 종목코드
            @JsonProperty("mdfy_qty") String modifyQuantity,      // 정정수량
            @JsonProperty("mdfy_uv") String modifyPrice,          // 정정단가
            @JsonProperty("mdfy_cond_uv") String modifyConditionPrice  // 정정조건단가
    ) {
        public static ModifyOrderRequest of(String orderNo, String stockCode, int quantity, double price) {
            return new ModifyOrderRequest(
                    "KRX",
                    orderNo,
                    stockCode,
                    String.valueOf(quantity),
                    String.valueOf(Math.round(price)),
                    null
            );
        }
    }

    public record OrderRequest(
            @JsonProperty("dmst_stex_tp") String marketType,      // 국내거래소구분 (KRX, NXT, SOR)
            @JsonProperty("stk_cd") String stockCode,             // 종목코드
            @JsonProperty("ord_qty") String orderQuantity,        // 주문수량
            @JsonProperty("ord_uv") String orderPrice,            // 주문단가
            @JsonProperty("trde_tp") String tradeType,            // 매매구분
            @JsonProperty("cond_uv") String conditionPrice        // 조건단가
    ) {
        // 매수주문 생성 - 시장가일 경우 단가 필드 생략
        public static OrderRequest createBuyOrder(String stockCode, int quantity, double price, String tradeType) {
            // 시장가 주문인 경우(tradeType="3") 단가 필드 null로 처리
            String priceStr = "3".equals(tradeType) ? null : String.valueOf(Math.round(price));

            return new OrderRequest(
                    "KRX",
                    stockCode,
                    String.valueOf(quantity),
                    priceStr,
                    tradeType,
                    null
            );
        }

        // 매도주문 생성 - 시장가일 경우 단가 필드 생략
        public static OrderRequest createSellOrder(String stockCode, int quantity, double price, String tradeType) {
            // 시장가 주문인 경우(tradeType="3") 단가 필드 null로 처리
            String priceStr = "3".equals(tradeType) ? null : String.valueOf(Math.round(price));

            return new OrderRequest(
                    "KRX",
                    stockCode,
                    String.valueOf(quantity),
                    priceStr,
                    tradeType,
                    null
            );
        }
    }

    private record OrderResponse(
            @JsonProperty("ord_no") String orderNo,                  // 주문번호
            @JsonProperty("dmst_stex_tp") String marketType,         // 국내거래소구분
            @JsonProperty("base_orig_ord_no") String baseOriginalOrderNo,  // 모주문번호 (정정/취소 시)
            @JsonProperty("mdfy_qty") String modifyQuantity,         // 정정수량 (정정 시)
            @JsonProperty("cncl_qty") String cancelQuantity,         // 취소수량 (취소 시)
            @JsonProperty("return_code") Integer returnCode,         // 응답 코드 (0: 정상)
            @JsonProperty("return_msg") String returnMessage         // 응답 메시지
    ) {

        /**
         * 응답이 정상인지 확인
         *
         * @return 정상 여부
         */
        public boolean isSuccess() {
            return returnCode != null && returnCode == 0;
        }
    }

    private record CancelOrderRequest(
            @JsonProperty("dmst_stex_tp") String marketType,      // 국내거래소구분 (KRX, NXT, SOR)
            @JsonProperty("orig_ord_no") String originalOrderNo,  // 원주문번호
            @JsonProperty("stk_cd") String stockCode,             // 종목코드
            @JsonProperty("cncl_qty") String cancelQuantity       // 취소수량
    ) {
        // 전량 취소는 "0"으로 설정
        public static CancelOrderRequest createFullCancel(String orderNo, String stockCode) {
            return new CancelOrderRequest(
                    "SOR",
                    orderNo,
                    stockCode,
                    "0"
            );
        }

        public static CancelOrderRequest createPartialCancel(String orderNo, String stockCode, int quantity) {
            return new CancelOrderRequest(
                    "SOR",
                    orderNo,
                    stockCode,
                    String.valueOf(quantity)
            );
        }
    }
}
