package com.kokimstocktrading.domain.monitoring;

import java.util.Objects;
import java.util.UUID;

/**
 * 가격 조건 도메인 모델
 */
public class PriceCondition {

  private final UUID id;
  private final String stockCode;
  private final Long targetPrice;
  private final TouchDirection touchDirection;
  private final Runnable callback;
  private final String description;

  public PriceCondition(UUID id, String stockCode, Long targetPrice, Runnable callback,
      TouchDirection touchDirection) {
    this(id, stockCode, targetPrice, touchDirection, callback, null);
  }

  public PriceCondition(UUID id, String stockCode, Long targetPrice, TouchDirection touchDirection,
      Runnable callback, String description) {
    if (stockCode == null || stockCode.trim().isEmpty()) {
      throw new IllegalArgumentException("종목코드는 필수입니다");
    }
    if (targetPrice == null || targetPrice <= 0) {
      throw new IllegalArgumentException("목표가격은 0보다 커야 합니다");
    }
    if (callback == null) {
      throw new IllegalArgumentException("콜백은 필수입니다");
    }

    this.id = id;
    this.touchDirection = touchDirection;
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
    if (touchDirection == TouchDirection.FROM_BELOW) {
      return currentPrice >= targetPrice;
    }
    if (touchDirection == TouchDirection.FROM_ABOVE) {
      return currentPrice <= targetPrice;
    }
    throw new IllegalArgumentException("지원하지 않는 TouchDirection 입니다");
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

  // Getters
  public UUID getId() {
    return id;
  }

  public String getStockCode() {
    return stockCode;
  }

  public Long getTargetPrice() {
    return targetPrice;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PriceCondition that = (PriceCondition) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return String.format("PriceCondition{id=%s, stockCode='%s', targetPrice=%d, description='%s'}",
        id, stockCode, targetPrice, description);
  }
}
