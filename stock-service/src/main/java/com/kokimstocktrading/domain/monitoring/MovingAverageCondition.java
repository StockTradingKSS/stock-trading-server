package com.kokimstocktrading.domain.monitoring;

import com.kokimstocktrading.domain.candle.CandleInterval;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * 이평선 기반 가격 조건 도메인 모델
 */
@Getter
public class MovingAverageCondition implements Condition {

  // Getters and Setters
  private final UUID id;
  private final String stockCode;
  private final int period;              // 이평선 기간 (예: 20일)
  private final CandleInterval interval; // 캔들 간격 (분, 일, 주, 월, 년)
  private final TouchDirection touchDirection;
  private final Runnable callback;
  private final String description;

  // 현재 활성화된 PriceCondition의 ID (동적으로 업데이트됨)
  private UUID currentPriceConditionId;

  // 조건 상태 (START: 감시중, SUCCESS: 조건 달성)
  @Setter
  private ConditionStatus status;

  public MovingAverageCondition(UUID uuid, String stockCode, int period, CandleInterval interval,
      Runnable callback, TouchDirection touchDirection) {
    this(uuid, stockCode, period, interval, touchDirection, callback, null, ConditionStatus.START);
  }

  public MovingAverageCondition(UUID uuid, String stockCode, int period, CandleInterval interval,
      TouchDirection touchDirection,
      Runnable callback, String description) {
    this(uuid, stockCode, period, interval, touchDirection, callback, description,
        ConditionStatus.START);
  }

  public MovingAverageCondition(UUID uuid, String stockCode, int period, CandleInterval interval,
      TouchDirection touchDirection,
      Runnable callback, String description, ConditionStatus status) {
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
    if (touchDirection == null) {
      throw new IllegalArgumentException("터치 방향은 필수입니다");
    }

    this.touchDirection = touchDirection;
    this.id = uuid;
    this.stockCode = stockCode;
    this.period = period;
    this.interval = interval;
    this.callback = callback;
    this.description = description != null ? description :
        String.format("%s %d%s 이평선 조건", stockCode, period, interval.getDisplayName());
    this.status = status != null ? status : ConditionStatus.START;
  }

  /**
   * 현재 이평선 가격으로 PriceCondition 생성
   */
  public PriceCondition createPriceCondition(Long movingAveragePrice, Runnable additionalCallback) {
    if (movingAveragePrice == null || movingAveragePrice <= 0) {
      throw new IllegalArgumentException("이평선 가격이 유효하지 않습니다: " + movingAveragePrice);
    }

    String conditionDescription = String.format("%s %d%s 이평선(%d원) 도달",
        stockCode, period, interval.getDisplayName(), movingAveragePrice);

    // 새로운 UUID를 생성하여 PriceCondition 생성 (MovingAverageCondition ID와 분리)
    UUID newPriceConditionId = UUID.randomUUID();
    return new PriceCondition(newPriceConditionId, stockCode, movingAveragePrice, touchDirection,
        () -> {
          callback.run();
          additionalCallback.run();
        },
        conditionDescription);
  }

  /**
   * 현재 활성화된 PriceCondition의 ID 설정
   */
  public void setCurrentPriceConditionId(UUID currentPriceConditionId) {
    this.currentPriceConditionId = currentPriceConditionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MovingAverageCondition that = (MovingAverageCondition) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return String.format(
        "MovingAverageCondition{id=%s, stockCode='%s', period=%d, interval=%s, description='%s'}",
        id, stockCode, period, interval, description);
  }

  public void success() {
    this.status = ConditionStatus.SUCCESS;
  }
}
