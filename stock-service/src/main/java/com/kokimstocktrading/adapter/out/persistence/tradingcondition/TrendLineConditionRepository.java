package com.kokimstocktrading.adapter.out.persistence.tradingcondition;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 추세선 조건 Repository
 */
@Repository
public interface TrendLineConditionRepository extends
    JpaRepository<TrendLineConditionEntity, UUID> {

  /**
   * 활성화된 모든 추세선 조건 조회
   */
  @Query("SELECT t FROM TrendLineConditionEntity t WHERE t.isActive = true ORDER BY t.createdAt DESC")
  List<TrendLineConditionEntity> findAllActive();

  /**
   * 특정 종목의 활성화된 추세선 조건 조회
   */
  @Query("SELECT t FROM TrendLineConditionEntity t WHERE t.stockCode = :stockCode AND t.isActive = true")
  List<TrendLineConditionEntity> findActiveByStockCode(String stockCode);
}
