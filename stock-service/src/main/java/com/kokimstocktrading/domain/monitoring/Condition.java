package com.kokimstocktrading.domain.monitoring;

import com.kokimstocktrading.domain.candle.CandleInterval;
import java.util.UUID;

/**
 * 거래 조건 인터페이스
 */
public interface Condition {

  /**
   * 조건 ID 조회
   */
  UUID getId();

  /**
   * 종목 코드 조회
   */
  String getStockCode();

  /**
   * 캔들 간격 조회
   */
  CandleInterval getInterval();

  /**
   * 터치 방향 조회
   */
  TouchDirection getTouchDirection();

  /**
   * 콜백 조회
   */
  Runnable getCallback();

  /**
   * 조건 설명 조회
   */
  String getDescription();

  /**
   * 조건 상태 조회
   */
  ConditionStatus getStatus();

  /**
   * 조건 상태 설정
   */
  void setStatus(ConditionStatus status);

  /**
   * 현재 활성화된 PriceCondition ID 조회
   */
  UUID getCurrentPriceConditionId();

  /**
   * 현재 활성화된 PriceCondition ID 설정
   */
  void setCurrentPriceConditionId(UUID priceConditionId);

  /**
   * 주어진 목표 가격으로 PriceCondition 생성
   */
  PriceCondition createPriceCondition(Long targetPrice, Runnable additionalCallback);
}
