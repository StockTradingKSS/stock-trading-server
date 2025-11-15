package com.kokimstocktrading.application.condition.port.out;

import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 거래 조건 저장 포트
 */
public interface SaveTradingConditionPort {

    /**
     * 이평선 조건 저장
     */
    MovingAverageCondition saveMovingAverageCondition(MovingAverageCondition condition);

    /**
     * 추세선 조건 저장
     */
    TrendLineCondition saveTrendLineCondition(TrendLineCondition condition);

    /**
     * 이평선 조건 삭제
     */
    void deleteMovingAverageCondition(UUID conditionId);

    /**
     * 추세선 조건 삭제
     */
    void deleteTrendLineCondition(UUID conditionId);

    /**
     * 이평선 조건 조회
     */
    Optional<MovingAverageCondition> findMovingAverageConditionById(UUID conditionId);

    /**
     * 추세선 조건 조회
     */
    Optional<TrendLineCondition> findTrendLineConditionById(UUID conditionId);

    /**
     * 모든 활성화된 이평선 조건 조회
     */
    List<MovingAverageCondition> findAllActiveMovingAverageConditions();

    /**
     * 모든 활성화된 추세선 조건 조회
     */
    List<TrendLineCondition> findAllActiveTrendLineConditions();
}
