package com.KimStock.adapter.out.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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