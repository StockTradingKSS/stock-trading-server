package com.KimStock.adapter.out.kiwoom.dto;

import com.KimStock.domain.model.type.MarketType;
import lombok.Builder;

import java.util.Objects;

/**
 * 업종코드 요청 DTO
 */
public record MarketCodeRequest(
    String mrkt_tp     // 0:코스피(거래소),1:코스닥,2:KOSPI200,4:KOSPI100,7:KRX100(통합지수)
) {
    @Builder
    public MarketCodeRequest {
        mrkt_tp = Objects.requireNonNullElse(mrkt_tp, "0");
    }

    public static MarketCodeRequest of(MarketType marketType) {
        // 0:코스피,10:코스닥,3:ELW,8:ETF,30:K-OTC,50:코넥스,5:신주인수권,4:뮤추얼펀드,6:리츠,9:하이일드
        String mrkt_tp = "0";

        switch (marketType){
            case KOSPI -> mrkt_tp = "0";
            case KOSDAQ -> mrkt_tp = "1";
            case null, default -> throw new IllegalArgumentException("marketType is null");
        }

        return MarketCodeRequest.builder()
                .mrkt_tp(mrkt_tp)
                .build();
    }
}
