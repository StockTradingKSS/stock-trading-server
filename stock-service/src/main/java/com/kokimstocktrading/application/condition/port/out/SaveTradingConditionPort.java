package com.kokimstocktrading.application.condition.port.out;

import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 거래 조건 저장 포트
 */
public interface SaveTradingConditionPort {

    /**
     * 이평선 조건 저장
     */
    Mono<MovingAverageCondition> saveMovingAverageCondition(MovingAverageCondition condition);

    /**
     * 추세선 조건 저장
     */
    Mono<TrendLineCondition> saveTrendLineCondition(TrendLineCondition condition);

    /**
     * 이평선 조건 삭제
     */
    Mono<Void> deleteMovingAverageCondition(UUID conditionId);

    /**
     * 추세선 조건 삭제
     */
    Mono<Void> deleteTrendLineCondition(UUID conditionId);

    /**
     * 이평선 조건 조회
     */
    Mono<MovingAverageCondition> findMovingAverageConditionById(UUID conditionId);

    /**
     * 추세선 조건 조회
     */
    Mono<TrendLineCondition> findTrendLineConditionById(UUID conditionId);

    /**
     * 모든 활성화된 이평선 조건 조회
     */
    Flux<MovingAverageCondition> findAllActiveMovingAverageConditions();

    /**
     * 모든 활성화된 추세선 조건 조회
     */
    Flux<TrendLineCondition> findAllActiveTrendLineConditions();
}
