package com.kokimstocktrading.adapter.out.persistence.tradingcondition;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 이평선 조건 Repository
 */
@Repository
public interface MovingAverageConditionRepository extends
    JpaRepository<MovingAverageConditionEntity, UUID> {

  /**
   * START 상태인 모든 이평선 조건 조회 (감시중인 조건만)
   */
  @Query("SELECT m FROM MovingAverageConditionEntity m WHERE m.status = 'START' ORDER BY m.createdAt DESC")
  List<MovingAverageConditionEntity> findAllActive();

  /**
   * 특정 종목의 START 상태인 이평선 조건 조회
   */
  @Query("SELECT m FROM MovingAverageConditionEntity m WHERE m.stockCode = :stockCode AND m.status = 'START'")
  List<MovingAverageConditionEntity> findActiveByStockCode(String stockCode);
}
