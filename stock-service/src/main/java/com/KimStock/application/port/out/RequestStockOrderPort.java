package com.KimStock.application.port.out;

import com.KimStock.domain.model.OrderResult;
import com.KimStock.domain.model.type.TradeType;
import reactor.core.publisher.Mono;

public interface RequestStockOrderPort {
    /**
     * 주식 매수 주문
     *
     * @param stockCode 종목코드
     * @param quantity  주문수량
     * @param price     주문단가
     * @param tradeType 주문유형 (LIMIT: 지정가, MARKET: 시장가)
     * @return 주문 결과
     */
    Mono<OrderResult> requestBuyStock(String stockCode, int quantity, double price, TradeType tradeType);

    /**
     * 주식 매도 주문
     *
     * @param stockCode 종목코드
     * @param quantity  주문수량
     * @param price     주문단가
     * @param tradeType 주문유형 (LIMIT: 지정가, MARKET: 시장가)
     * @return 주문 결과
     */
    Mono<OrderResult> requestSellStock(String stockCode, int quantity, double price, TradeType tradeType);

    /**
     * 주문 정정
     *
     * @param orderNo   원주문번호
     * @param stockCode 종목코드
     * @param quantity  정정수량
     * @param price     정정단가
     * @return 주문 결과
     */
    Mono<OrderResult> requestModifyOrder(String orderNo, String stockCode, int quantity, double price);

    /**
     * 주문 취소 (전량)
     *
     * @param orderNo   원주문번호
     * @param stockCode 종목코드
     * @return 주문 결과
     */
    Mono<OrderResult> requestCancelOrder(String orderNo, String stockCode);

    /**
     * 주문 취소 (부분)
     *
     * @param orderNo   원주문번호
     * @param stockCode 종목코드
     * @param quantity  취소수량
     * @return 주문 결과
     */
    Mono<OrderResult> requestCancelOrder(String orderNo, String stockCode, int quantity);
}
