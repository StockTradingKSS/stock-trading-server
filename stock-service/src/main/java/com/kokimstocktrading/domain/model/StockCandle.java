package com.kokimstocktrading.domain.model;

import com.kokimstocktrading.domain.model.type.CandleInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class StockCandle {
    private String code;       // 종목코드
    private CandleInterval candleInterval;
    private Long currentPrice; // 현재가
    private Long previousPrice;// 전봉의 종가
    private Long volume;       // 거래량
    private Long openPrice;    // 시가
    private Long highPrice;    // 고가
    private Long lowPrice;     // 저가
    private Long closePrice;   // 종가
    private LocalDateTime openTime; // 시간
}
