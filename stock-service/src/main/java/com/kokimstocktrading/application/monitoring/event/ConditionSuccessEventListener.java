package com.kokimstocktrading.application.monitoring.event;

import com.kokimstocktrading.application.condition.port.out.SaveTradingConditionPort;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 조건 성공 이벤트 리스너 - ConditionSuccessEvent를 수신하여 DB 상태를 SUCCESS로 업데이트
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConditionSuccessEventListener {

  private final SaveTradingConditionPort saveTradingConditionPort;

  /**
   * 조건 성공 이벤트 처리 - 비동기로 DB 상태를 SUCCESS로 업데이트
   */
  @EventListener
  @Async
  public void handleConditionSuccessEvent(ConditionSuccessEvent event) {
    log.info("조건 성공 이벤트 수신: {}", event);

    try {
      switch (event.conditionType()) {
        case MOVING_AVERAGE -> {
          MovingAverageCondition movingAverageCondition = saveTradingConditionPort.findMovingAverageConditionById(
                  event.conditionId())
              .orElseThrow(NotFoundException::new);
          movingAverageCondition.success();
          saveTradingConditionPort.saveMovingAverageCondition(movingAverageCondition);
          log.info("이평선 조건 SUCCESS 상태로 업데이트 완료: {}", event.conditionId());
        }
        case TREND_LINE -> {
          TrendLineCondition trendLineCondition = saveTradingConditionPort.findTrendLineConditionById(
                  event.conditionId())
              .orElseThrow(NotFoundException::new);
          trendLineCondition.success();
          saveTradingConditionPort.saveTrendLineCondition(trendLineCondition);
          log.info("추세선 조건 SUCCESS 상태로 업데이트 완료: {}", event.conditionId());
        }
      }
    } catch (Exception e) {
      log.error("조건 성공 이벤트 처리 중 오류 발생: {}", event, e);
    }
  }
}
