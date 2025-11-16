package com.kokimstocktrading.application.condition.port.in;

import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;

import java.util.UUID;

/**
 * 거래 조건 등록 Use Case
 */
public interface RegisterTradingConditionUseCase {

    /**
     * 이평선 조건 등록
     */
    MovingAverageCondition registerMovingAverageCondition(RegisterMovingAverageCommand command);

    /**
     * 추세선 조건 등록
     */
    TrendLineCondition registerTrendLineCondition(RegisterTrendLineCommand command);

    /**
     * 조건 삭제
     */
    void deleteCondition(UUID conditionId);
}
