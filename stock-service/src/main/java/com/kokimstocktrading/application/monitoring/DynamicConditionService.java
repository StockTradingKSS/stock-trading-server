package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.application.monitoring.calculator.MovingAverageTouchPriceCalculator;
import com.kokimstocktrading.application.monitoring.calculator.TrendLineTouchPriceCalculator;
import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.PriceCondition;
import com.kokimstocktrading.domain.monitoring.TouchDirection;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * 동적 가격 조건 관리 서비스 (이평선 등)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicConditionService {

    private final MovingAverageTouchPriceCalculator movingAverageTouchPriceCalculator;
    private final TrendLineTouchPriceCalculator trendLineTouchPriceCalculator;
    private final MonitorPriceService monitorPriceService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // 등록된 이평선 조건들 (조건 ID -> 이평선 조건)
    private final Map<UUID, MovingAverageCondition> movingAverageConditions = new ConcurrentHashMap<>();

    // 등록된 추세선 조건들 (조건 ID -> 추세선 조건)
    private final Map<UUID, TrendLineCondition> trendLineConditions = new ConcurrentHashMap<>();

    // 주기적 업데이트 스케줄러 (조건 ID -> Disposable)
    private final Map<UUID, Disposable> updateSchedulers = new ConcurrentHashMap<>();

    /**
     * 업데이트 간격 제공자 설정
     */
    @Setter
    private Function<CandleInterval, Duration> updateIntervalProvider = this::getUpdateInterval;
    
    /**
     * 초기 지연 계산 제공자 설정 (테스트용)
     */
    @Setter
    private Function<Duration, Long> initialDelayProvider = this::calculateInitialDelay;

    @PreDestroy
    public void destroy() {
        // 먼저 모든 스케줄러 중지
        updateSchedulers.values().forEach(disposable -> {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
        });
        updateSchedulers.clear();
        
        // 모든 조건 제거
        movingAverageConditions.clear();
        trendLineConditions.clear();
        
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
        
        log.info("DynamicConditionService 종료 완료");
    }

    /**
     * 이평선 조건 등록 (설명 포함)
     */
    public Mono<MovingAverageCondition> registerMovingAverageCondition(
            String stockCode, int period, CandleInterval interval, TouchDirection touchDirection, Runnable callback, String description) {
        UUID uuid = UUID.randomUUID();
        MovingAverageCondition condition = new MovingAverageCondition(
                uuid, stockCode, period, interval, touchDirection, () -> {
            callback.run();
            Disposable disposable = updateSchedulers.remove(uuid);
            if (disposable != null) {
                disposable.dispose();
            }
        }, description);

        return initializeMovingAverageCondition(condition)
                .doOnSuccess(initializedCondition -> {
                    movingAverageConditions.put(condition.getId(), condition);
                    startPeriodicUpdate(condition);
                    log.info("이평선 조건 등록 완료: {}", condition);
                })
                .doOnError(error -> log.error("이평선 조건 등록 실패: {}", condition, error));
    }

    /**
     * 이평선 조건 초기화 (첫 번째 PriceCondition 생성)
     */
    private Mono<MovingAverageCondition> initializeMovingAverageCondition(MovingAverageCondition condition) {
        return movingAverageTouchPriceCalculator.calculateTouchPrice(
                        condition.getStockCode(), condition.getPeriod(), condition.getInterval())
                .map(movingAveragePrice -> {
                    PriceCondition priceCondition = condition.createPriceCondition(movingAveragePrice);
                    PriceCondition registered = monitorPriceService.registerPriceCondition(priceCondition);

                    log.info("초기 이평선 가격 조건 생성: 종목={}, 이평선가격={}, 조건ID={}",
                            condition.getStockCode(), movingAveragePrice, registered.getId());

                    return condition;
                });
    }

    /**
     * 주기적 이평선 업데이트 시작
     */
    private void startPeriodicUpdate(MovingAverageCondition condition) {
        Duration updateInterval = updateIntervalProvider.apply(condition.getInterval());
        
        // 정각 실행을 위한 초기 지연 계산
        long initialDelayMs = initialDelayProvider.apply(updateInterval);
        long periodMs = updateInterval.toMillis();

        Disposable scheduler = Flux.defer(() ->
                        Mono.fromRunnable(() ->
                                updateMovingAverageCondition(condition)
                                        .doOnError(error -> log.error("이평선 조건 업데이트 중 오류: {}", condition, error))
                                        .onErrorContinue((error, obj) -> log.warn("이평선 업데이트 오류, 계속 진행: {}", error.getMessage()))
                                        .subscribe()
                        )
                ).repeat()
                .delayElements(Duration.ofMillis(periodMs))
                .delaySubscription(Duration.ofMillis(initialDelayMs))
                .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(this.scheduler))
                .subscribe();

        updateSchedulers.put(condition.getId(), scheduler);
        log.info("이평선 주기적 업데이트 시작: 조건={}, 간격={}, 첫 실행까지={}ms", 
            condition.getId(), updateInterval, initialDelayMs);
    }

    /**
     * 정각 실행을 위한 초기 지연 계산
     */
    private long calculateInitialDelay(Duration interval) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextExecution;
        
        if (interval.equals(Duration.ofMinutes(1))) {
            // 1분 간격: 다음 분의 00초
            nextExecution = now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
        } else if (interval.equals(Duration.ofHours(1))) {
            // 1시간 간격: 다음 시간의 00분 00초
            nextExecution = now.plusHours(1).truncatedTo(ChronoUnit.HOURS);
        } else {
            // 기타 간격: 현재 시각부터 해당 간격만큼 후
            nextExecution = now.plus(interval);
        }
        
        return Duration.between(now, nextExecution).toMillis();
    }

    /**
     * 이평선 조건 업데이트 (기존 조건 삭제 후 새 조건 생성)
     */
    private Mono<Void> updateMovingAverageCondition(MovingAverageCondition condition) {
        return movingAverageTouchPriceCalculator.calculateTouchPrice(
                        condition.getStockCode(), condition.getPeriod(), condition.getInterval())
                .flatMap(newMovingAveragePrice -> {
                    // 기존 PriceCondition 삭제
                    UUID oldConditionId = condition.getId();
                    if (oldConditionId != null) {
                        boolean removed = monitorPriceService.removePriceCondition(oldConditionId);
                        log.debug("기존 이평선 조건 삭제: 조건ID={}, 성공={}", oldConditionId, removed);
                    }

                    // 새로운 PriceCondition 생성
                    PriceCondition newPriceCondition = condition.createPriceCondition(newMovingAveragePrice);
                    PriceCondition registered = monitorPriceService.registerPriceCondition(newPriceCondition);

                    log.info("이평선 조건 업데이트: 종목={}, 새 이평선가격={}, 새 조건ID={}",
                            condition.getStockCode(), newMovingAveragePrice, registered.getId());

                    return Mono.<Void>empty();
                })
                .onErrorResume(error -> {
                    log.error("이평선 조건 업데이트 실패: {}", condition, error);
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

    /**
     * 이평선 조건 삭제
     */
    public boolean removeMovingAverageCondition(UUID conditionId) {
        MovingAverageCondition condition = movingAverageConditions.remove(conditionId);
        if (condition == null) {
            log.warn("존재하지 않는 이평선 조건 ID: {}", conditionId);
            return false;
        }

        // 주기적 업데이트 중지
        Disposable scheduler = updateSchedulers.remove(conditionId);
        if (scheduler != null && !scheduler.isDisposed()) {
            scheduler.dispose();
        }

        // 현재 활성화된 PriceCondition 삭제
        UUID currentPriceConditionId = condition.getId();
        if (currentPriceConditionId != null) {
            monitorPriceService.removePriceCondition(currentPriceConditionId);
        }

        log.info("이평선 조건 삭제 완료: {}", condition);
        return true;
    }

    /**
     * 모든 이평선 조건 삭제
     */
    public void removeAllMovingAverageConditions() {
        movingAverageConditions.keySet().forEach(this::removeMovingAverageCondition);
        log.info("모든 이평선 조건 삭제 완료");
    }

    /**
     * 등록된 이평선 조건 개수 조회
     */
    public int getMovingAverageConditionCount() {
        return movingAverageConditions.size();
    }

    // ================================ 추세선 조건 관리 ================================

    /**
     * 추세선 조건 등록 및 주기적 업데이트 시작
     */
    public Mono<TrendLineCondition> registerTrendLineCondition(
            String stockCode, LocalDateTime toDate, BigDecimal slope, CandleInterval interval, Runnable callback) {

        return registerTrendLineCondition(stockCode, toDate, slope, interval, TouchDirection.FROM_ABOVE, callback, null);
    }

    /**
     * 추세선 조건 등록 (설명 포함)
     */
    public Mono<TrendLineCondition> registerTrendLineCondition(
            String stockCode, LocalDateTime toDate, BigDecimal slope,
            CandleInterval interval, TouchDirection touchDirection, Runnable callback, String description) {

        TrendLineCondition condition = new TrendLineCondition(
                stockCode, toDate, slope, interval, touchDirection, callback, description);

        return initializeTrendLineCondition(condition)
                .doOnSuccess(initializedCondition -> {
                    trendLineConditions.put(condition.getId(), condition);
                    startTrendLinePeriodicUpdate(condition);
                    log.info("추세선 조건 등록 완료: {}", condition);
                })
                .doOnError(error -> log.error("추세선 조건 등록 실패: {}", condition, error));
    }

    /**
     * 추세선 조건 초기화 (첫 번째 PriceCondition 생성)
     */
    private Mono<TrendLineCondition> initializeTrendLineCondition(TrendLineCondition condition) {
        return trendLineTouchPriceCalculator.calculateTouchPrice(
                        condition.getStockCode(), condition.getToDate(), condition.getSlope(), condition.getInterval())
                .map(trendLinePrice -> {
                    PriceCondition priceCondition = condition.createPriceCondition(trendLinePrice);
                    PriceCondition registered = monitorPriceService.registerPriceCondition(priceCondition);
                    condition.setCurrentPriceConditionId(registered.getId());

                    log.info("초기 추세선 가격 조건 생성: 종목={}, 추세선가격={}, 조건ID={}",
                            condition.getStockCode(), trendLinePrice, registered.getId());

                    return condition;
                });
    }

    /**
     * 주기적 추세선 업데이트 시작
     */
    private void startTrendLinePeriodicUpdate(TrendLineCondition condition) {
        Duration updateInterval = updateIntervalProvider.apply(condition.getInterval());
        
        // 정각 실행을 위한 초기 지연 계산
        long initialDelayMs = initialDelayProvider.apply(updateInterval);
        long periodMs = updateInterval.toMillis();

        Disposable scheduler = Flux.defer(() ->
                        Mono.fromRunnable(() ->
                                updateTrendLineCondition(condition)
                                        .doOnError(error -> log.error("추세선 조건 업데이트 중 오류: {}", condition, error))
                                        .onErrorContinue((error, obj) -> log.warn("추세선 업데이트 오류, 계속 진행: {}", error.getMessage()))
                                        .subscribe()
                        )
                ).repeat()
                .delayElements(Duration.ofMillis(periodMs))
                .delaySubscription(Duration.ofMillis(initialDelayMs))
                .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(this.scheduler))
                .subscribe();

        updateSchedulers.put(condition.getId(), scheduler);
        log.info("추세선 주기적 업데이트 시작: 조건={}, 간격={}, 첫 실행까지={}ms", 
            condition.getId(), updateInterval, initialDelayMs);
    }

    /**
     * 추세선 조건 업데이트 (기존 조건 삭제 후 새 조건 생성)
     */
    private Mono<Void> updateTrendLineCondition(TrendLineCondition condition) {
        return trendLineTouchPriceCalculator.calculateTouchPrice(
                        condition.getStockCode(), condition.getToDate(), condition.getSlope(), condition.getInterval())
                .flatMap(newTrendLinePrice -> {
                    // 기존 PriceCondition 삭제
                    UUID oldConditionId = condition.getCurrentPriceConditionId();
                    if (oldConditionId != null) {
                        boolean removed = monitorPriceService.removePriceCondition(oldConditionId);
                        log.debug("기존 추세선 조건 삭제: 조건ID={}, 성공={}", oldConditionId, removed);
                    }

                    // 새로운 PriceCondition 생성
                    PriceCondition newPriceCondition = condition.createPriceCondition(newTrendLinePrice);
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
     * 추세선 조건 삭제
     */
    public boolean removeTrendLineCondition(UUID conditionId) {
        TrendLineCondition condition = trendLineConditions.remove(conditionId);
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

        log.info("추세선 조건 삭제 완료: {}", condition);
        return true;
    }

    /**
     * 모든 추세선 조건 삭제
     */
    public void removeAllTrendLineConditions() {
        trendLineConditions.keySet().forEach(this::removeTrendLineCondition);
        log.info("모든 추세선 조건 삭제 완료");
    }

    /**
     * 등록된 추세선 조건 개수 조회
     */
    public int getTrendLineConditionCount() {
        return trendLineConditions.size();
    }

    /**
     * 모든 조건 삭제 (이평선 + 추세선)
     */
    public void removeAllConditions() {
        removeAllMovingAverageConditions();
        removeAllTrendLineConditions();
        log.info("모든 동적 조건 삭제 완료");
    }
}
