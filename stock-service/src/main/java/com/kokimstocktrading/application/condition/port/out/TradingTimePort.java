package com.kokimstocktrading.application.condition.port.out;

/**
 * 거래 시간 확인 포트
 */
public interface TradingTimePort {

  /**
   * 현재 거래 시간인지 확인
   */
  boolean isTradingTime();

  /**
   * 장 시작 시간인지 확인 (07:50 ~ 08:00)
   */
  boolean isMarketOpenTime();

  /**
   * 장 종료 시간인지 확인 (20:10 ~ 20:20)
   */
  boolean isMarketCloseTime();
}
