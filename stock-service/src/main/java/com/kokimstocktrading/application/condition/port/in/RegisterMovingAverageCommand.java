package com.kokimstocktrading.application.condition.port.in;

import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.TouchDirection;

/**
 * 이평선 조건 등록 커맨드
 */
public record RegisterMovingAverageCommand(
    String stockCode,
    int period,
    CandleInterval interval,
    TouchDirection touchDirection,
    String description
) {

  public RegisterMovingAverageCommand {
    if (stockCode == null || stockCode.trim().isEmpty()) {
      throw new IllegalArgumentException("종목코드는 필수입니다");
    }
    if (period <= 0) {
      throw new IllegalArgumentException("이평선 기간은 0보다 커야 합니다");
    }
    if (interval == null) {
      throw new IllegalArgumentException("캔들 간격은 필수입니다");
    }
  }
}
