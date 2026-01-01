package com.kokimstocktrading.application.monitoring.dynamiccondition;

import com.kokimstocktrading.application.monitoring.MonitorPriceService;
import com.kokimstocktrading.application.monitoring.calculator.TrendLineTouchPriceCalculator;
import com.kokimstocktrading.application.monitoring.event.ConditionSuccessEvent;
import com.kokimstocktrading.application.monitoring.event.ConditionSuccessEvent.ConditionType;
import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.PriceCondition;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 추세선 동적 조건 관리 서비스
 */
@Service
@Slf4j
public class TrendLineDynamicCondition implements DynamicCondition<TrendLineCondition> {

  private final TrendLineTouchPriceCalculator trendLineTouchPriceCalculator;
  private final MonitorPriceService monitorPriceService;
  private final ApplicationEventPublisher eventPublisher;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

  // 등록된 추세선 조건들 (조건 ID -> 추세선 조건)
  private final Map<UUID, TrendLineCondition> conditions = new ConcurrentHashMap<>();

  // 주기적 업데이트 스케줄러 (조건 ID -> Disposable)
  private final Map<UUID, Disposable> updateSchedulers = new ConcurrentHashMap<>();

  /**
   * 업데이트 간격 제공자 설정
   */
  @Setter
  private Function<CandleInterval, Duration> updateIntervalProvider = this::getUpdateInterval;

  /**
   * 초기 지연 계산 제공자 설정
   */
  @Setter
  private Function<Duration, Long> initialDelayProvider = this::calculateInitialDelay;

  public TrendLineDynamicCondition(
      TrendLineTouchPriceCalculator trendLineTouchPriceCalculator,
      MonitorPriceService monitorPriceService,
      ApplicationEventPublisher eventPublisher) {
    this.trendLineTouchPriceCalculator = trendLineTouchPriceCalculator;
    this.monitorPriceService = monitorPriceService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Mono<TrendLineCondition> registerCondition(TrendLineCondition condition) {

    return initializeCondition(condition, () -> removeCondition(condition.getId()))
        .doOnSuccess(unused -> {
          conditions.put(condition.getId(), condition);
          startPeriodicUpdate(condition);
          log.info("추세선 조건 등록 완료: {}", condition);
        })
        .doOnError(error -> log.error("추세선 조건 등록 실패: {}", condition, error));
  }

  /**
   * 추세선 조건 초기화 (첫 번째 PriceCondition 생성)
   */
  @Override
  public Mono<TrendLineCondition> initializeCondition(TrendLineCondition condition,
      Runnable additionalCallback) {
    // 기존에 등록된 PriceCondition이 있다면 제거
    UUID oldConditionId = condition.getCurrentPriceConditionId();
    if (oldConditionId != null) {
      boolean removed = monitorPriceService.removePriceCondition(oldConditionId);
      log.debug("초기화 시 기존 추세선 조건 삭제: 조건ID={}, 성공={}", oldConditionId, removed);
    }

    return trendLineTouchPriceCalculator.calculateTargetPrice(
            condition.getStockCode(), condition.getBaseDate(), condition.getBasePrice(), condition.getSlope(),
            condition.getInterval())
        .map(targetPrice -> {
          PriceCondition priceCondition = condition.createPriceCondition(targetPrice,
              additionalCallback);
          PriceCondition registered = monitorPriceService.registerPriceCondition(priceCondition);
          condition.setCurrentPriceConditionId(registered.getId());

          log.info("초기 추세선 가격 조건 생성: 종목={}, 추세선가격={}, 조건ID={}",
              condition.getStockCode(), targetPrice, registered.getId());

          return condition;
        });
  }

  /**
   * 주기적 추세선 업데이트 시작
   */
  private void startPeriodicUpdate(TrendLineCondition condition) {
    Duration updateInterval = updateIntervalProvider.apply(condition.getInterval());

    // 정각 실행을 위한 초기 지연 계산
    long initialDelayMs = initialDelayProvider.apply(updateInterval);
    long periodMs = updateInterval.toMillis();

    Disposable scheduler = Flux.interval(
            Duration.ofMillis(initialDelayMs),
            Duration.ofMillis(periodMs),
            reactor.core.scheduler.Schedulers.fromExecutor(this.scheduler)
        )
        .flatMap(tick -> updateCondition(condition)
            .doOnError(error -> log.error("추세선 조건 업데이트 중 오류: {}", condition, error))
            .onErrorResume(error -> {
              log.warn("추세선 업데이트 오류, 계속 진행: {}", error.getMessage());
              return Mono.empty();
            })
        )
        .subscribe();

    updateSchedulers.put(condition.getId(), scheduler);
    log.info("추세선 주기적 업데이트 시작: 조건={}, 간격={}, 첫 실행까지={}ms",
        condition.getId(), updateInterval, initialDelayMs);
  }

  /**
   * 추세선 조건 업데이트 (기존 조건 삭제 후 새 조건 생성)
   */
  private Mono<Void> updateCondition(TrendLineCondition condition) {
    return trendLineTouchPriceCalculator.calculateTargetPrice(
            condition.getStockCode(), condition.getBaseDate(), condition.getBasePrice(), condition.getSlope(),
            condition.getInterval())
        .flatMap(newTrendLinePrice -> {
          // 기존 PriceCondition 삭제
          UUID oldConditionId = condition.getCurrentPriceConditionId();
          if (oldConditionId != null) {
            boolean removed = monitorPriceService.removePriceCondition(oldConditionId);
            log.debug("기존 추세선 조건 삭제: 조건ID={}, 성공={}", oldConditionId, removed);
          }

          // 새로운 PriceCondition 생성
          PriceCondition newPriceCondition = condition.createPriceCondition(newTrendLinePrice,
              () -> removeCondition(condition.getId()));
          PriceCondition registered = monitorPriceService.registerPriceCondition(newPriceCondition);
          condition.setCurrentPriceConditionId(registered.getId());

          log.info("추세선 조건 업데이트: 종목={}, 새 추세선가격={}, 새 조건ID={}",
              condition.getStockCode(), newTrendLinePrice, registered.getId());

          return Mono.<Void>empty();
        })
        .onErrorResume(error -> {
          log.error("추세선 조건 업데이트 실패: {}", condition, error);
          return Mono.empty(); // 오류 시에도 스케줄러 계속 동작
        });
  }

  /**
   * 캔들 간격에 따른 업데이트 주기 결정
   */
  private Duration getUpdateInterval(CandleInterval interval) {
    return switch (interval) {
      case MINUTE -> Duration.ofMinutes(1);   // 1분마다 업데이트
      case DAY -> Duration.ofHours(1);        // 1시간마다 업데이트 (실시간성 고려)
      case WEEK -> Duration.ofHours(6);       // 6시간마다 업데이트
      case MONTH -> Duration.ofHours(24);     // 24시간마다 업데이트
      case YEAR -> Duration.ofDays(7);        // 7일마다 업데이트
    };
  }

  @Override
  public boolean removeCondition(UUID conditionId) {
    TrendLineCondition condition = conditions.remove(conditionId);
    if (condition == null) {
      log.warn("존재하지 않는 추세선 조건 ID: {}", conditionId);
      return false;
    }

    // 주기적 업데이트 중지
    Disposable scheduler = updateSchedulers.remove(conditionId);
    if (scheduler != null && !scheduler.isDisposed()) {
      scheduler.dispose();
    }

    // 현재 활성화된 PriceCondition 삭제
    UUID currentPriceConditionId = condition.getCurrentPriceConditionId();
    if (currentPriceConditionId != null) {
      monitorPriceService.removePriceCondition(currentPriceConditionId);
    }

    // 조건 성공 이벤트 발행 (DB 상태를 SUCCESS로 업데이트)
    eventPublisher.publishEvent(new ConditionSuccessEvent(conditionId, ConditionType.TREND_LINE));
    log.info("추세선 조건 삭제 완료 및 성공 이벤트 발행: {}", condition);

    return true;
  }

  @Override
  public void removeAllConditions() {
    conditions.keySet().forEach(this::removeCondition);
    log.info("모든 추세선 조건 삭제 완료");
  }

  @Override
  public int getConditionCount() {
    return conditions.size();
  }

  public void destroy() {
    // 먼저 모든 스케줄러 중지
    updateSchedulers.values().forEach(disposable -> {
      if (disposable != null && !disposable.isDisposed()) {
        disposable.dispose();
      }
    });
    updateSchedulers.clear();

    // 모든 조건 제거
    conditions.clear();

    // 스케줄러 종료
    try {
      scheduler.shutdown();
      if (!scheduler.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }

    log.info("TrendLineDynamicCondition 종료 완료");
  }
}
