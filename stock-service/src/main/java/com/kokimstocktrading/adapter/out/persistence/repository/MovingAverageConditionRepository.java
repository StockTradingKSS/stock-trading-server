package com.kokimstocktrading.adapter.out.persistence.repository;

import com.kokimstocktrading.adapter.out.persistence.entity.MovingAverageConditionEntity;
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
   * 활성화된 모든 이평선 조건 조회
   */
  @Query("SELECT m FROM MovingAverageConditionEntity m WHERE m.isActive = true ORDER BY m.createdAt DESC")
  List<MovingAverageConditionEntity> findAllActive();

  /**
   * 특정 종목의 활성화된 이평선 조건 조회
   */
  @Query("SELECT m FROM MovingAverageConditionEntity m WHERE m.stockCode = :stockCode AND m.isActive = true")
  List<MovingAverageConditionEntity> findActiveByStockCode(String stockCode);
}
