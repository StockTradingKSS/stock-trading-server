package com.kokimstocktrading.domain.monitoring;

import com.kokimstocktrading.domain.candle.CandleInterval;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/**
 * 이평선 기반 가격 조건 도메인 모델
 */
@Getter
public class MovingAverageCondition {

    // Getters and Setters
    private final UUID id;
    private final String stockCode;
    private final int period;              // 이평선 기간 (예: 20일)
    private final CandleInterval interval; // 캔들 간격 (분, 일, 주, 월, 년)
    private final Runnable callback;
    private final String description;
    
    // 현재 활성화된 PriceCondition ID (업데이트 시 삭제용)
    @Setter
    private UUID currentPriceConditionId;

    public MovingAverageCondition(String stockCode, int period, CandleInterval interval, Runnable callback) {
        this(stockCode, period, interval, callback, null);
    }

    public MovingAverageCondition(String stockCode, int period, CandleInterval interval, 
                                 Runnable callback, String description) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new IllegalArgumentException("종목코드는 필수입니다");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("이평선 기간은 0보다 커야 합니다");
        }
        if (interval == null) {
            throw new IllegalArgumentException("캔들 간격은 필수입니다");
        }
        if (callback == null) {
            throw new IllegalArgumentException("콜백은 필수입니다");
        }

        this.id = UUID.randomUUID();
        this.stockCode = stockCode;
        this.period = period;
        this.interval = interval;
        this.callback = callback;
        this.description = description != null ? description : 
            String.format("%s %d%s 이평선 조건", stockCode, period, interval.getDisplayName());
    }

    /**
     * 현재 이평선 가격으로 PriceCondition 생성
     */
    public PriceCondition createPriceCondition(Long movingAveragePrice) {
        if (movingAveragePrice == null || movingAveragePrice <= 0) {
            throw new IllegalArgumentException("이평선 가격이 유효하지 않습니다: " + movingAveragePrice);
        }

        String conditionDescription = String.format("%s %d%s 이평선(%d원) 도달", 
            stockCode, period, interval.getDisplayName(), movingAveragePrice);
        
        return new PriceCondition(stockCode, movingAveragePrice, callback, conditionDescription);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovingAverageCondition that = (MovingAverageCondition) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("MovingAverageCondition{id=%s, stockCode='%s', period=%d, interval=%s, description='%s'}", 
                id, stockCode, period, interval, description);
    }
}
