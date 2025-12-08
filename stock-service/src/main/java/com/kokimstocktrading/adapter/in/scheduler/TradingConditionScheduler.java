package com.kokimstocktrading.adapter.in.scheduler;

import com.kokimstocktrading.application.condition.TradingConditionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 거래 조건 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TradingConditionScheduler {

  private final TradingConditionService tradingConditionService;

  /**
   * 매일 오전 7시 50분에 모든 활성화된 거래 조건을 DynamicConditionService에 등록
   */
  @Scheduled(cron = "0 50 7 * * *", zone = "Asia/Seoul")
  public void registerActiveTradingConditions() {
    log.info("거래 조건 등록 스케줄러 시작");

    try {
      tradingConditionService.registerAllActiveConditions()
          .doOnSuccess(unused -> log.info("모든 활성화된 거래 조건 등록 완료"))
          .doOnError(error -> log.error("거래 조건 등록 실패", error))
          .block();
    } catch (Exception e) {
      log.error("거래 조건 등록 중 예외 발생", e);
    }
  }

  /**
   * 매일 오후 20시 10분에 모든 거래 조건 모니터링을 종료
   */
  @Scheduled(cron = "0 10 20 * * *", zone = "Asia/Seoul")
  public void stopAllTradingConditions() {
    log.info("거래 조건 종료 스케줄러 시작");

    try {
      tradingConditionService.unregisterAllConditions()
          .doOnSuccess(unused -> log.info("모든 거래 조건 종료 완료"))
          .doOnError(error -> log.error("모든 거래 조건 종료  실패", error))
          .block();
    } catch (Exception e) {
      log.error("모든 거래 조건 종료  중 예외 발생", e);
    }
  }
}
