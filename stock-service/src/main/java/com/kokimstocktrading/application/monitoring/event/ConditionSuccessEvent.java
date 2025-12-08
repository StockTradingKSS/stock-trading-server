package com.kokimstocktrading.application.monitoring.event;

import java.util.UUID;

/**
 * 조건 성공 이벤트 - 이평선 또는 추세선 조건이 달성되어 제거될 때 발행 - DB 상태를 SUCCESS로 업데이트하기 위해 사용
 */
public record ConditionSuccessEvent(
    UUID conditionId,
    ConditionType conditionType
) {

  /**
   * 조건 타입 (이평선 또는 추세선)
   */
  public enum ConditionType {
    MOVING_AVERAGE,
    TREND_LINE
  }
}
