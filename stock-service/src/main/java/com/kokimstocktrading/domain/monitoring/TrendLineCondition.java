package com.kokimstocktrading.domain.monitoring;

import com.kokimstocktrading.domain.candle.CandleInterval;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 추세선 기반 가격 조건 도메인 모델
 */
@Getter
public class TrendLineCondition {

    private final UUID id;
    private final String stockCode;
    private final LocalDateTime toDate;        // 추세선 끝점 날짜
    private final BigDecimal slope;            // 추세선 기울기
    private final CandleInterval interval;     // 캔들 간격 (분, 일, 주, 월, 년)
    private final Runnable callback;
    private final String description;
    
    // 현재 활성화된 PriceCondition ID (업데이트 시 삭제용)
    @Setter
    private UUID currentPriceConditionId;

    public TrendLineCondition(String stockCode, LocalDateTime toDate, BigDecimal slope, 
                             CandleInterval interval, Runnable callback) {
        this(stockCode, toDate, slope, interval, callback, null);
    }

    public TrendLineCondition(String stockCode, LocalDateTime toDate, BigDecimal slope, 
                             CandleInterval interval, Runnable callback, String description) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new IllegalArgumentException("종목코드는 필수입니다");
        }
        if (toDate == null) {
            throw new IllegalArgumentException("추세선 끝점 날짜는 필수입니다");
        }
        if (slope == null) {
            throw new IllegalArgumentException("추세선 기울기는 필수입니다");
        }
        if (interval == null) {
            throw new IllegalArgumentException("캔들 간격은 필수입니다");
        }
        if (callback == null) {
            throw new IllegalArgumentException("콜백은 필수입니다");
        }

        this.id = UUID.randomUUID();
        this.stockCode = stockCode;
        this.toDate = toDate;
        this.slope = slope;
        this.interval = interval;
        this.callback = callback;
        this.description = description != null ? description : 
            String.format("%s 추세선(기울기:%.2f) 조건", stockCode, slope);
    }

    /**
     * 현재 추세선 가격으로 PriceCondition 생성
     */
    public PriceCondition createPriceCondition(Long trendLinePrice) {
        if (trendLinePrice == null || trendLinePrice <= 0) {
            throw new IllegalArgumentException("추세선 가격이 유효하지 않습니다: " + trendLinePrice);
        }

        String conditionDescription = String.format("%s 추세선(기울기:%.2f, %d원) 도달", 
            stockCode, slope, trendLinePrice);
        
        return new PriceCondition(stockCode, trendLinePrice, callback, conditionDescription);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrendLineCondition that = (TrendLineCondition) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("TrendLineCondition{id=%s, stockCode='%s', toDate=%s, slope=%.2f, interval=%s, description='%s'}", 
                id, stockCode, toDate, slope, interval, description);
    }
}
