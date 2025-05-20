package com.KimStock.adapter.out.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CancelOrderRequest(
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
