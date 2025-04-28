package com.KimStock.adapter.out.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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