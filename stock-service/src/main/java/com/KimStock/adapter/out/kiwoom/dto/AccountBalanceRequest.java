package com.KimStock.adapter.out.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountBalanceRequest(
    @JsonProperty("qry_tp") String queryType,  // 조회구분 (1:합산, 2:개별)
    @JsonProperty("dmst_stex_tp") String domesticStockExchangeType  // 국내거래소구분 (KRX:한국거래소, NXT:넥스트트레이드)
) {
    public static AccountBalanceRequest getDefaultRequest() {
        return new AccountBalanceRequest("1", "KRX");
    }
}
