package com.kokimstocktrading.domain.monitoring;

import com.kokimstocktrading.domain.candle.CandleInterval;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * 추세선 기반 가격 조건 도메인 모델
 */
@Getter
public class TrendLineCondition implements Condition {

  private final UUID id;
  private final String stockCode;
  private final LocalDateTime baseDate;        // 추세선 끝점 날짜
  private final Long basePrice;        // 추세선 끝점 날짜
  private final BigDecimal slope;            // 추세선 기울기
  private final CandleInterval interval;     // 캔들 간격 (분, 일, 주, 월, 년)
  private final TouchDirection touchDirection;     // 캔들 간격 (분, 일, 주, 월, 년)
  private final Runnable callback;
  private final String description;

  // 현재 활성화된 PriceCondition ID (업데이트 시 삭제용)
  @Setter
  private UUID currentPriceConditionId;

  // 조건 상태 (START: 감시중, SUCCESS: 조건 달성)
  @Setter
  private ConditionStatus status;

  public TrendLineCondition(String stockCode, LocalDateTime baseDate, Long basePrice, BigDecimal slope,
      CandleInterval interval, TouchDirection touchDirection, Runnable callback,
      String description) {
    this(UUID.randomUUID(), stockCode, baseDate, basePrice, slope, interval, touchDirection, callback,
        description, ConditionStatus.START);
  }

  public TrendLineCondition(UUID id, String stockCode, LocalDateTime baseDate, Long basePrice, BigDecimal slope,
      CandleInterval interval, TouchDirection touchDirection, Runnable callback,
      String description, ConditionStatus status) {
    this.basePrice = basePrice;
    this.touchDirection = touchDirection;
    if (stockCode == null || stockCode.trim().isEmpty()) {
      throw new IllegalArgumentException("종목코드는 필수입니다");
    }
    if (baseDate == null) {
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

    this.id = id;
    this.stockCode = stockCode;
    this.baseDate = baseDate;
    this.slope = slope;
    this.interval = interval;
    this.callback = callback;
    this.description = description != null ? description :
        String.format("%s 추세선(기울기:%.2f) 조건", stockCode, slope);
    this.status = status != null ? status : ConditionStatus.START;
  }

  /**
   * 현재 추세선 가격으로 PriceCondition 생성
   */
  public PriceCondition createPriceCondition(Long trendLinePrice, Runnable additionalCallback) {
    if (trendLinePrice == null || trendLinePrice <= 0) {
      throw new IllegalArgumentException("추세선 가격이 유효하지 않습니다: " + trendLinePrice);
    }

    String conditionDescription = String.format("%s 추세선(기울기:%.2f, %d원) 도달",
        stockCode, slope, trendLinePrice);

    return new PriceCondition(id, stockCode, trendLinePrice, touchDirection, () -> {
      callback.run();
      additionalCallback.run();
    },
        conditionDescription);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrendLineCondition that = (TrendLineCondition) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return String.format(
        "TrendLineCondition{id=%s, stockCode='%s', baseDate=%s, slope=%.2f, interval=%s, description='%s'}",
        id, stockCode, baseDate, slope, interval, description);
  }

  public void success() {
    this.status = ConditionStatus.SUCCESS;
  }
}
