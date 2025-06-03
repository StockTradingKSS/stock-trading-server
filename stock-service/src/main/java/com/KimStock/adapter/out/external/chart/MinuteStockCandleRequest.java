package com.KimStock.adapter.out.external.chart;

import lombok.Builder;

import java.util.Objects;

public record MinuteStockCandleRequest(
        String stk_cd, // 종목 코드
        String tic_scope, //틱범위
        String upd_stkpc_tp // 수정주가구분
) {
    @Builder
    public MinuteStockCandleRequest {
        stk_cd = Objects.requireNonNullElse(stk_cd, "039490");
    }

    public static MinuteStockCandleRequest of(String stockCode, Integer ticScope, boolean isUpdatedPrice) {
        return MinuteStockCandleRequest.builder()
                .stk_cd(stockCode+"_AL")
                .tic_scope(String.valueOf(ticScope))
                .upd_stkpc_tp(String.valueOf(isUpdatedPrice? 1 : 0))
                .build();
    }
}
