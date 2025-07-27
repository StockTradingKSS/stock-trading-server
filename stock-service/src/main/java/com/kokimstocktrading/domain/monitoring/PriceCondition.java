package com.kokimstocktrading.domain.monitoring;

import lombok.Getter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 가격 조건 도메인 모델
 */
public class PriceCondition {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    // Getters
    @Getter
    private final Long id;
    @Getter
    private final String stockCode;
    @Getter
    private final Long targetPrice;
    private final Runnable callback;
    @Getter
    private final String description;

    public PriceCondition(String stockCode, Long targetPrice, Runnable callback) {
        this(stockCode, targetPrice, callback, null);
    }

    public PriceCondition(String stockCode, Long targetPrice, Runnable callback, String description) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new IllegalArgumentException("종목코드는 필수입니다");
        }
        if (targetPrice == null || targetPrice <= 0) {
            throw new IllegalArgumentException("목표가격은 0보다 커야 합니다");
        }
        if (callback == null) {
            throw new IllegalArgumentException("콜백은 필수입니다");
        }

        this.id = ID_GENERATOR.incrementAndGet();
        this.stockCode = stockCode;
        this.targetPrice = targetPrice;
        this.callback = callback;
        this.description = description != null ? description :
            String.format("%s %d원 도달", stockCode, targetPrice);
    }

    /**
     * 현재 가격이 목표 가격에 도달했는지 확인
     */
    public boolean isAchieved(double currentPrice) {
        return currentPrice >= targetPrice;
    }

    /**
     * 조건 달성 시 콜백 실행
     */
    public void executeCallback() {
        try {
            callback.run();
        } catch (Exception e) {
            // 콜백 실행 중 오류가 발생해도 모니터링은 계속됨
            System.err.println("조건 콜백 실행 중 오류: " + e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceCondition that = (PriceCondition) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("PriceCondition{id=%d, stockCode='%s', targetPrice=%d, description='%s'}",
                id, stockCode, targetPrice, description);
    }
}
