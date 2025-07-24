package com.kokimstocktrading.domain.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OrderResult {
    private final String orderNo;            // 주문번호
    private final String originalOrderNo;    // 원주문번호 (정정/취소 시)
    private final String stockCode;          // 종목코드
    private final String marketType;         // 시장구분
    private final OrderType orderType;       // 주문유형
    private final int quantity;              // 수량
    private final String resultMessage;      // 결과 메시지

    public enum OrderType {
        BUY, SELL, MODIFY, CANCEL
    }

    public static OrderResult createBuyResult(String orderNo, String stockCode, String marketType, int quantity, String resultMessage) {
        return OrderResult.builder()
                .orderNo(orderNo)
                .stockCode(stockCode)
                .marketType(marketType)
                .orderType(OrderType.BUY)
                .quantity(quantity)
                .resultMessage(resultMessage)
                .build();
    }

    public static OrderResult createSellResult(String orderNo, String stockCode, String marketType, int quantity, String resultMessage) {
        return OrderResult.builder()
                .orderNo(orderNo)
                .stockCode(stockCode)
                .marketType(marketType)
                .orderType(OrderType.SELL)
                .quantity(quantity)
                .resultMessage(resultMessage)
                .build();
    }

    public static OrderResult createModifyResult(String orderNo, String originalOrderNo, String stockCode, String marketType, int quantity, String resultMessage) {
        return OrderResult.builder()
                .orderNo(orderNo)
                .originalOrderNo(originalOrderNo)
                .stockCode(stockCode)
                .marketType(marketType)
                .orderType(OrderType.MODIFY)
                .quantity(quantity)
                .resultMessage(resultMessage)
                .build();
    }

    public static OrderResult createCancelResult(String orderNo, String originalOrderNo, String stockCode, int quantity, String resultMessage) {
        return OrderResult.builder()
                .orderNo(orderNo)
                .originalOrderNo(originalOrderNo)
                .stockCode(stockCode)
                .orderType(OrderType.CANCEL)
                .quantity(quantity)
                .resultMessage(resultMessage)
                .build();
    }
}
