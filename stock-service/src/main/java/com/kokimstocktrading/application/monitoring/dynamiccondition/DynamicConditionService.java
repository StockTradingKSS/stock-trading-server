package com.kokimstocktrading.application.monitoring.dynamiccondition;

import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 동적 가격 조건 관리 서비스 (파사드) - MovingAverageDynamicCondition과 TrendLineDynamicCondition을 조합하여 사용
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicConditionService {

  private final MovingAverageDynamicCondition movingAverageDynamicCondition;
  private final TrendLineDynamicCondition trendLineDynamicCondition;

  @PreDestroy
  public void destroy() {
    movingAverageDynamicCondition.destroy();
    trendLineDynamicCondition.destroy();
    log.info("DynamicConditionService 종료 완료");
  }

  // ================================ 이평선 조건 관리 ================================

  /**
   * 이평선 조건 등록 (외부에서 생성된 조건 사용)
   */
  public Mono<MovingAverageCondition> registerMovingAverageCondition(
      MovingAverageCondition condition) {
    return movingAverageDynamicCondition.registerCondition(condition);
  }

  /**
   * 이평선 조건 삭제
   */
  public boolean removeMovingAverageCondition(UUID conditionId) {
    return movingAverageDynamicCondition.removeCondition(conditionId);
  }

  /**
   * 모든 이평선 조건 삭제
   */
  public void removeAllMovingAverageConditions() {
    movingAverageDynamicCondition.removeAllConditions();
  }

  /**
   * 등록된 이평선 조건 개수 조회
   */
  public int getMovingAverageConditionCount() {
    return movingAverageDynamicCondition.getConditionCount();
  }

  /**
   * 업데이트 간격 제공자 설정 (테스트용) - 이평선과 추세선 모두에 적용
   */
  public void setUpdateIntervalProvider(Function<CandleInterval, Duration> updateIntervalProvider) {
    movingAverageDynamicCondition.setUpdateIntervalProvider(updateIntervalProvider);
    trendLineDynamicCondition.setUpdateIntervalProvider(updateIntervalProvider);
  }

  /**
   * 초기 지연 계산 제공자 설정 (테스트용) - 이평선과 추세선 모두에 적용
   */
  public void setInitialDelayProvider(Function<Duration, Long> initialDelayProvider) {
    movingAverageDynamicCondition.setInitialDelayProvider(initialDelayProvider);
    trendLineDynamicCondition.setInitialDelayProvider(initialDelayProvider);
  }

  // ================================ 추세선 조건 관리 ================================

  /**
   * 추세선 조건 등록 (외부에서 생성된 조건 사용)
   */
  public Mono<TrendLineCondition> registerTrendLineCondition(TrendLineCondition condition) {
    return trendLineDynamicCondition.registerCondition(condition);
  }

  /**
   * 추세선 조건 삭제
   */
  public boolean removeTrendLineCondition(UUID conditionId) {
    return trendLineDynamicCondition.removeCondition(conditionId);
  }

  /**
   * 모든 추세선 조건 삭제
   */
  public void removeAllTrendLineConditions() {
    trendLineDynamicCondition.removeAllConditions();
  }

  /**
   * 등록된 추세선 조건 개수 조회
   */
  public int getTrendLineConditionCount() {
    return trendLineDynamicCondition.getConditionCount();
  }

  /**
   * 모든 조건 삭제 (이평선 + 추세선)
   */
  public void removeAllConditions() {
    removeAllMovingAverageConditions();
    removeAllTrendLineConditions();
    log.info("모든 동적 조건 삭제 완료");
  }
}
