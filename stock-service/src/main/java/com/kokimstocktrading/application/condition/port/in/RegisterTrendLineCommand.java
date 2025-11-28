package com.kokimstocktrading.application.condition.port.in;

import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.TouchDirection;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 추세선 조건 등록 커맨드
 */
public record RegisterTrendLineCommand(
    String stockCode,
    LocalDateTime toDate,
    BigDecimal slope,
    TouchDirection touchDirection,
    CandleInterval interval,
    String description
) {

  public RegisterTrendLineCommand {
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
  }
}
