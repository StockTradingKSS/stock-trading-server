package com.KimStock.adapter.in.web.stock;

import com.KimStock.domain.model.Stock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSearchResponse {
    private String code;                  // 종목코드
    private String name;                  // 종목명
    private Long listCount;               // 상장주식수
    private String marketCode;            // 시장코드
    private String marketName;            // 시장명
    private String upName;                // 업종명

    // 도메인 모델을 응답 DTO로 변환
    public static StockSearchResponse fromDomain(Stock stock) {
        return StockSearchResponse.builder()
                .code(stock.getCode())
                .name(stock.getName())
                .listCount(stock.getListCount())
                .marketCode(stock.getMarketCode())
                .marketName(stock.getMarketName())
                .upName(stock.getUpName())
                .build();
    }
}
