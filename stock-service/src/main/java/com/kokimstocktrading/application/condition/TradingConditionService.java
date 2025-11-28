package com.kokimstocktrading.application.condition;

import com.kokimstocktrading.application.condition.port.in.RegisterMovingAverageCommand;
import com.kokimstocktrading.application.condition.port.in.RegisterTradingConditionUseCase;
import com.kokimstocktrading.application.condition.port.in.RegisterTrendLineCommand;
import com.kokimstocktrading.application.condition.port.out.SaveTradingConditionPort;
import com.kokimstocktrading.application.condition.port.out.TradingTimePort;
import com.kokimstocktrading.application.monitoring.DynamicConditionService;
import com.kokimstocktrading.application.notification.port.out.SendNotificationPort;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 거래 조건 관리 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradingConditionService implements RegisterTradingConditionUseCase {

  private final SaveTradingConditionPort saveTradingConditionPort;
  private final TradingTimePort tradingTimePort;
  private final DynamicConditionService dynamicConditionService;
  private final SendNotificationPort sendNotificationPort;

  @Override
  @Transactional
  public MovingAverageCondition registerMovingAverageCondition(
      RegisterMovingAverageCommand command) {
    log.info("이평선 조건 등록 요청: 종목={}, 기간={}, 간격={}",
        command.stockCode(), command.period(), command.interval());

    // 1. 도메인 객체 생성 (callback 포함)
    MovingAverageCondition condition = new MovingAverageCondition(
        UUID.randomUUID(),
        command.stockCode(),
        command.period(),
        command.interval(),
        command.touchDirection(),
        () -> handleMovingAverageConditionTriggered(command.stockCode(), command.description()),
        command.description()
    );

    // 2. DB에 저장
    MovingAverageCondition savedCondition = saveTradingConditionPort.saveMovingAverageCondition(
        condition);
    log.info("이평선 조건 DB 저장 완료: id={}", savedCondition.getId());

    // 3. 거래 시간이면 DynamicConditionService에 등록
    if (tradingTimePort.isTradingTime()) {
      log.info("거래 시간이므로 모니터링 서비스에 등록: {}", savedCondition.getId());
      try {
        dynamicConditionService.registerMovingAverageCondition(
            savedCondition.getStockCode(),
            savedCondition.getPeriod(),
            savedCondition.getInterval(),
            savedCondition.getTouchDirection(),
            savedCondition.getCallback(),
            savedCondition.getDescription()
        ).block();
        log.info("이평선 조건 모니터링 등록 완료: {}", savedCondition.getId());
      } catch (Exception e) {
        log.error("이평선 조건 모니터링 등록 실패", e);
        throw new RuntimeException("모니터링 등록 실패", e);
      }
    } else {
      log.info("거래 시간이 아니므로 모니터링 서비스 등록 생략: {}", savedCondition.getId());
    }

    return savedCondition;
  }

  @Override
  @Transactional
  public TrendLineCondition registerTrendLineCondition(RegisterTrendLineCommand command) {
    log.info("추세선 조건 등록 요청: 종목={}, 끝점={}, 기울기={}, 간격={}",
        command.stockCode(), command.toDate(), command.slope(), command.interval());

    // 1. 도메인 객체 생성 (callback 포함)
    TrendLineCondition condition = new TrendLineCondition(
        command.stockCode(),
        command.toDate(),
        command.slope(),
        command.interval(),
        command.touchDirection(),
        () -> handleTrendLineConditionTriggered(command.stockCode(), command.description()),
        command.description()
    );

    // 2. DB에 저장
    TrendLineCondition savedCondition = saveTradingConditionPort.saveTrendLineCondition(condition);
    log.info("추세선 조건 DB 저장 완료: id={}", savedCondition.getId());

    // 3. 거래 시간이면 DynamicConditionService에 등록
    if (tradingTimePort.isTradingTime()) {
      log.info("거래 시간이므로 모니터링 서비스에 등록: {}", savedCondition.getId());
      try {
        dynamicConditionService.registerTrendLineCondition(
            savedCondition.getStockCode(),
            savedCondition.getToDate(),
            savedCondition.getSlope(),
            savedCondition.getInterval(),
            savedCondition.getTouchDirection(),
            savedCondition.getCallback(),
            savedCondition.getDescription()
        ).block();
        log.info("추세선 조건 모니터링 등록 완료: {}", savedCondition.getId());
      } catch (Exception e) {
        log.error("추세선 조건 모니터링 등록 실패", e);
        throw new RuntimeException("모니터링 등록 실패", e);
      }
    } else {
      log.info("거래 시간이 아니므로 모니터링 서비스 등록 생략: {}", savedCondition.getId());
    }

    return savedCondition;
  }

  @Override
  @Transactional
  public void deleteCondition(UUID conditionId) {
    log.info("조건 삭제 요청: {}", conditionId);

    // 1. 이평선 조건 삭제 시도
    try {
      saveTradingConditionPort.deleteMovingAverageCondition(conditionId);
      log.info("이평선 조건 DB 삭제 완료: {}", conditionId);

      // DynamicConditionService에서도 제거
      dynamicConditionService.removeMovingAverageCondition(conditionId);
      log.info("이평선 조건 모니터링 제거 완료: {}", conditionId);
      return;
    } catch (Exception e) {
      log.debug("이평선 조건으로 삭제 실패, 추세선 조건 시도: {}", conditionId);
    }

    // 2. 추세선 조건 삭제 시도
    try {
      saveTradingConditionPort.deleteTrendLineCondition(conditionId);
      log.info("추세선 조건 DB 삭제 완료: {}", conditionId);

      // DynamicConditionService에서도 제거
      dynamicConditionService.removeTrendLineCondition(conditionId);
      log.info("추세선 조건 모니터링 제거 완료: {}", conditionId);
    } catch (Exception e) {
      log.error("조건 삭제 실패: {}", conditionId, e);
      throw new IllegalArgumentException("존재하지 않는 조건 ID: " + conditionId);
    }
  }

  /**
   * 이평선 조건 발동 시 처리
   */
  private void handleMovingAverageConditionTriggered(String stockCode, String description) {
    log.info("이평선 조건 발동: 종목={}, 설명={}", stockCode, description);

    String message = String.format("[이평선 조건 발동]\n종목: %s\n설명: %s", stockCode, description);

    sendNotificationPort.sendMessage(message)
        .doOnSuccess(unused -> log.info("알림 전송 완료: {}", stockCode))
        .doOnError(error -> log.error("알림 전송 실패: {}", stockCode, error))
        .subscribe();
  }

  /**
   * 추세선 조건 발동 시 처리
   */
  private void handleTrendLineConditionTriggered(String stockCode, String description) {
    log.info("추세선 조건 발동: 종목={}, 설명={}", stockCode, description);

    String message = String.format("[추세선 조건 발동]\n종목: %s\n설명: %s", stockCode, description);

    sendNotificationPort.sendMessage(message)
        .doOnSuccess(unused -> log.info("알림 전송 완료: {}", stockCode))
        .doOnError(error -> log.error("알림 전송 실패: {}", stockCode, error))
        .subscribe();
  }

  /**
   * 모든 활성화된 조건을 모니터링 서비스에 등록 앱 시작 시 호출됨
   */
  public Mono<Void> registerAllActiveConditions() {
    log.info("모든 활성화된 조건을 모니터링 서비스에 등록 시작");

    // 1. 이평선 조건 등록
    Mono<Void> registerMovingAverages = Flux.fromIterable(
        saveTradingConditionPort.findAllActiveMovingAverageConditions()
    ).flatMap(condition -> {
      // DB에서 조회한 조건은 callback이 비어있으므로, 재생성
      MovingAverageCondition conditionWithCallback = new MovingAverageCondition(
          condition.getId(),
          condition.getStockCode(),
          condition.getPeriod(),
          condition.getInterval(),
          condition.getTouchDirection(),
          () -> handleMovingAverageConditionTriggered(condition.getStockCode(),
              condition.getDescription()),
          condition.getDescription()
      );

      return dynamicConditionService.registerMovingAverageCondition(
          conditionWithCallback.getStockCode(),
          conditionWithCallback.getPeriod(),
          conditionWithCallback.getInterval(),
          conditionWithCallback.getTouchDirection(),
          conditionWithCallback.getCallback(),
          conditionWithCallback.getDescription()
      );
    }).then();

    // 2. 추세선 조건 등록
    Mono<Void> registerTrendLines = Flux.fromIterable(
        saveTradingConditionPort.findAllActiveTrendLineConditions()
    ).flatMap(condition -> {
      // DB에서 조회한 조건은 callback이 비어있으므로, 재생성
      TrendLineCondition conditionWithCallback = new TrendLineCondition(
          condition.getId(),
          condition.getStockCode(),
          condition.getToDate(),
          condition.getSlope(),
          condition.getInterval(),
          condition.getTouchDirection(),
          () -> handleTrendLineConditionTriggered(condition.getStockCode(),
              condition.getDescription()),
          condition.getDescription()
      );

      return dynamicConditionService.registerTrendLineCondition(
          conditionWithCallback.getStockCode(),
          conditionWithCallback.getToDate(),
          conditionWithCallback.getSlope(),
          conditionWithCallback.getInterval(),
          conditionWithCallback.getTouchDirection(),
          conditionWithCallback.getCallback(),
          conditionWithCallback.getDescription()
      );
    }).then();

    return Mono.when(registerMovingAverages, registerTrendLines)
        .doOnSuccess(unused -> log.info("모든 활성화된 조건 등록 완료"))
        .doOnError(error -> log.error("활성화된 조건 등록 실패", error));
  }

  /**
   * 모든 조건을 모니터링 서비스에서 제거
   */
  public Mono<Void> unregisterAllConditions() {
    log.info("모든 조건을 모니터링 서비스에서 제거");

    dynamicConditionService.removeAllConditions();
    return Mono.empty().then();
  }
}
