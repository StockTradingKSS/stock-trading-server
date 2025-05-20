package com.KimStock.adapter.out.kiwoom.dto;

import com.KimStock.domain.model.type.MarketType;
import lombok.Builder;

import java.util.Objects;

/**
 * 종목 정보 요청 DTO
 */
public record StockInfoRequest(
    String mrkt_tp     // 시장구분(0:전체, 1:코스피, 2:코스닥)
) {
    @Builder
    public StockInfoRequest {
        mrkt_tp = Objects.requireNonNullElse(mrkt_tp, "0");
    }

    public static StockInfoRequest of(MarketType marketType) {
        // 0:코스피,10:코스닥,3:ELW,8:ETF,30:K-OTC,50:코넥스,5:신주인수권,4:뮤추얼펀드,6:리츠,9:하이일드
        String mrkt_tp = "0";

        switch (marketType){
            case KOSPI -> mrkt_tp = "0";
            case KOSDAQ -> mrkt_tp = "10";
            case null, default -> throw new IllegalArgumentException("marketType is null");
        }

        return StockInfoRequest.builder()
                .mrkt_tp(mrkt_tp)
                .build();
    }
}
