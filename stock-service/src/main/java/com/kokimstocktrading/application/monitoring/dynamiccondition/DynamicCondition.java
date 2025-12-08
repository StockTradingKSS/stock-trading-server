package com.kokimstocktrading.application.monitoring.dynamiccondition;

import com.kokimstocktrading.domain.monitoring.Condition;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 동적 조건 관리 인터페이스
 *
 * @param <T> 관리할 조건 타입 (Condition을 구현한 타입)
 */
public interface DynamicCondition<T extends Condition> {

  /**
   * 조건 등록 (외부에서 생성된 조건 사용) - 외부에서 생성된 condition 객체를 받아서 callback에 remove 로직 추가
   */
  Mono<T> registerCondition(T condition);

  /**
   * 조건 삭제
   */
  boolean removeCondition(UUID conditionId);

  /**
   * 모든 조건 삭제
   */
  void removeAllConditions();

  /**
   * 등록된 조건 개수 조회
   */
  int getConditionCount();

  /**
   * 이평선 조건 초기화 (첫 번째 PriceCondition 생성)
   */
  Mono<T> initializeCondition(T condition, Runnable deleteCallback);

  /**
   * 정각 실행을 위한 초기 지연 계산
   */
  default long calculateInitialDelay(Duration interval) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nextExecution;

    if (interval.equals(Duration.ofMinutes(1))) {
      // 1분 간격: 다음 분의 00초
      nextExecution = now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
    } else if (interval.equals(Duration.ofHours(1))) {
      // 1시간 간격: 다음 시간의 00분 00초
      nextExecution = now.plusHours(1).truncatedTo(ChronoUnit.HOURS);
    } else {
      // 기타 간격: 현재 시각부터 해당 간격만큼 후
      nextExecution = now.plus(interval);
    }

    return Duration.between(now, nextExecution).toMillis();
  }
}
