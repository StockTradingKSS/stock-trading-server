package com.kokimstocktrading.adapter.out.persistence.tradingcondition;

import com.kokimstocktrading.application.condition.port.out.SaveTradingConditionPort;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래 조건 영속성 어댑터
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TradingConditionPersistenceAdapter implements SaveTradingConditionPort {

  private final MovingAverageConditionRepository movingAverageConditionRepository;
  private final TrendLineConditionRepository trendLineConditionRepository;

  /**
   * 이평선 조건 저장
   */
  @Override
  @Transactional
  public MovingAverageCondition saveMovingAverageCondition(MovingAverageCondition condition) {
    log.debug("이평선 조건 저장 시작: {}", condition.getId());

    try {
      // 도메인 → 엔티티 변환
      MovingAverageConditionEntity entity = MovingAverageConditionEntity.create(
          condition.getStockCode(),
          condition.getPeriod(),
          condition.getInterval(),
          condition.getTouchDirection(),
          condition.getDescription()
      );

      // DB 저장
      MovingAverageConditionEntity saved = movingAverageConditionRepository.save(entity);
      log.info("이평선 조건 저장 완료: id={}, stockCode={}, period={}",
          saved.getId(), saved.getStockCode(), saved.getPeriod());

      // 엔티티 → 도메인 변환 (원본 callback 유지)
      return saved.toDomain(condition.getCallback());
    } catch (Exception e) {
      log.error("이평선 조건 저장 실패: {}", condition.getId(), e);
      throw e;
    }
  }

  /**
   * 추세선 조건 저장
   */
  @Override
  @Transactional
  public TrendLineCondition saveTrendLineCondition(TrendLineCondition condition) {
    log.debug("추세선 조건 저장 시작: {}", condition.getId());

    try {
      // 도메인 → 엔티티 변환
      TrendLineConditionEntity entity = TrendLineConditionEntity.create(
          condition.getStockCode(),
          condition.getToDate(),
          condition.getSlope(),
          condition.getInterval(),
          condition.getTouchDirection(),
          condition.getDescription()
      );

      // DB 저장
      TrendLineConditionEntity saved = trendLineConditionRepository.save(entity);
      log.info("추세선 조건 저장 완료: id={}, stockCode={}, slope={}",
          saved.getId(), saved.getStockCode(), saved.getSlope());

      // 엔티티 → 도메인 변환 (원본 callback 유지)
      return saved.toDomain(condition.getCallback());
    } catch (Exception e) {
      log.error("추세선 조건 저장 실패: {}", condition.getId(), e);
      throw e;
    }
  }

  /**
   * 이평선 조건 삭제 (논리 삭제 - 비활성화)
   */
  @Override
  @Transactional
  public void deleteMovingAverageCondition(UUID conditionId) {
    log.debug("이평선 조건 삭제 시작: {}", conditionId);

    try {
      movingAverageConditionRepository.findById(conditionId)
          .ifPresentOrElse(
              entity -> {
                entity.deactivate();
                movingAverageConditionRepository.save(entity);
                log.info("이평선 조건 비활성화 완료: {}", conditionId);
              },
              () -> log.warn("이평선 조건을 찾을 수 없음: {}", conditionId)
          );
    } catch (Exception e) {
      log.error("이평선 조건 삭제 실패: {}", conditionId, e);
      throw e;
    }
  }

  /**
   * 추세선 조건 삭제 (논리 삭제 - 비활성화)
   */
  @Override
  @Transactional
  public void deleteTrendLineCondition(UUID conditionId) {
    log.debug("추세선 조건 삭제 시작: {}", conditionId);

    try {
      trendLineConditionRepository.findById(conditionId)
          .ifPresentOrElse(
              entity -> {
                entity.deactivate();
                trendLineConditionRepository.save(entity);
                log.info("추세선 조건 비활성화 완료: {}", conditionId);
              },
              () -> log.warn("추세선 조건을 찾을 수 없음: {}", conditionId)
          );
    } catch (Exception e) {
      log.error("추세선 조건 삭제 실패: {}", conditionId, e);
      throw e;
    }
  }

  /**
   * 이평선 조건 조회 (ID) NOTE: callback은 빈 Runnable로 반환됨 (DB에 저장되지 않음)
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<MovingAverageCondition> findMovingAverageConditionById(UUID conditionId) {
    log.debug("이평선 조건 조회 시작: {}", conditionId);

    try {
      Optional<MovingAverageCondition> result = movingAverageConditionRepository.findById(
              conditionId)
          .map(entity -> entity.toDomain(() -> {
            // DB에서 조회한 경우 callback이 없으므로 빈 Runnable 제공
            log.warn("이평선 조건 {}에 callback이 없습니다. 재등록이 필요합니다.", conditionId);
          }));

      if (result.isPresent()) {
        log.debug("이평선 조건 조회 완료: {}", conditionId);
      } else {
        log.debug("이평선 조건을 찾을 수 없음: {}", conditionId);
      }

      return result;
    } catch (Exception e) {
      log.error("이평선 조건 조회 실패: {}", conditionId, e);
      throw e;
    }
  }

  /**
   * 추세선 조건 조회 (ID) NOTE: callback은 빈 Runnable로 반환됨 (DB에 저장되지 않음)
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<TrendLineCondition> findTrendLineConditionById(UUID conditionId) {
    log.debug("추세선 조건 조회 시작: {}", conditionId);

    try {
      Optional<TrendLineCondition> result = trendLineConditionRepository.findById(conditionId)
          .map(entity -> entity.toDomain(() -> {
            // DB에서 조회한 경우 callback이 없으므로 빈 Runnable 제공
            log.warn("추세선 조건 {}에 callback이 없습니다. 재등록이 필요합니다.", conditionId);
          }));

      if (result.isPresent()) {
        log.debug("추세선 조건 조회 완료: {}", conditionId);
      } else {
        log.debug("추세선 조건을 찾을 수 없음: {}", conditionId);
      }

      return result;
    } catch (Exception e) {
      log.error("추세선 조건 조회 실패: {}", conditionId, e);
      throw e;
    }
  }

  /**
   * 모든 활성화된 이평선 조건 조회 NOTE: callback은 빈 Runnable로 반환됨
   */
  @Override
  @Transactional(readOnly = true)
  public List<MovingAverageCondition> findAllActiveMovingAverageConditions() {
    log.debug("활성화된 이평선 조건 전체 조회 시작");

    try {
      List<MovingAverageCondition> result = movingAverageConditionRepository.findAllActive()
          .stream()
          .map(entity -> entity.toDomain(() -> {
            log.warn("이평선 조건 {}에 callback이 없습니다. 재등록이 필요합니다.", entity.getId());
          }))
          .collect(Collectors.toList());

      log.debug("활성화된 이평선 조건 전체 조회 완료: {} 건", result.size());
      return result;
    } catch (Exception e) {
      log.error("활성화된 이평선 조건 조회 실패", e);
      throw e;
    }
  }

  /**
   * 모든 활성화된 추세선 조건 조회 NOTE: callback은 빈 Runnable로 반환됨
   */
  @Override
  @Transactional(readOnly = true)
  public List<TrendLineCondition> findAllActiveTrendLineConditions() {
    log.debug("활성화된 추세선 조건 전체 조회 시작");

    try {
      List<TrendLineCondition> result = trendLineConditionRepository.findAllActive()
          .stream()
          .map(entity -> entity.toDomain(() -> {
            log.warn("추세선 조건 {}에 callback이 없습니다. 재등록이 필요합니다.", entity.getId());
          }))
          .collect(Collectors.toList());

      log.debug("활성화된 추세선 조건 전체 조회 완료: {} 건", result.size());
      return result;
    } catch (Exception e) {
      log.error("활성화된 추세선 조건 조회 실패", e);
      throw e;
    }
  }
}
