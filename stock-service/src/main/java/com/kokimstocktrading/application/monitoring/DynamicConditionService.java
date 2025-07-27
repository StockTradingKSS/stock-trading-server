package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.PriceCondition;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 동적 가격 조건 관리 서비스 (이평선 등)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicConditionService {

    private final MovingAverageTouchPriceCalculator movingAverageTouchPriceCalculator;
    private final MonitorPriceService monitorPriceService;

    // 등록된 이평선 조건들 (조건 ID -> 이평선 조건)
    private final Map<UUID, MovingAverageCondition> movingAverageConditions = new ConcurrentHashMap<>();
    
    // 주기적 업데이트 스케줄러 (조건 ID -> Disposable)
    private final Map<UUID, Disposable> updateSchedulers = new ConcurrentHashMap<>();

    /**
     * -- SETTER --
     *  업데이트 간격 제공자 설정
     */
    @Setter
    private Function<CandleInterval, Duration> updateIntervalProvider = this::getUpdateInterval;

    /**
     * 이평선 조건 등록 및 주기적 업데이트 시작
     */
    public Mono<MovingAverageCondition> registerMovingAverageCondition(
            String stockCode, int period, CandleInterval interval, Runnable callback) {
        
        return registerMovingAverageCondition(stockCode, period, interval, callback, null);
    }

    /**
     * 이평선 조건 등록 (설명 포함)
     */
    public Mono<MovingAverageCondition> registerMovingAverageCondition(
            String stockCode, int period, CandleInterval interval, Runnable callback, String description) {
        
        MovingAverageCondition condition = new MovingAverageCondition(
                stockCode, period, interval, callback, description);
        
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
                    condition.setCurrentPriceConditionId(registered.getId());
                    
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
        
        Disposable scheduler = Flux.interval(updateInterval)
                .flatMap(tick -> updateMovingAverageCondition(condition))
                .doOnError(error -> log.error("이평선 조건 업데이트 중 오류: {}", condition, error))
                .onErrorContinue((error, obj) -> {
                    // 오류가 발생해도 업데이트 계속 시도
                    log.warn("이평선 업데이트 오류, 계속 진행: {}", error.getMessage());
                })
                .subscribe();
        
        updateSchedulers.put(condition.getId(), scheduler);
        log.info("이평선 주기적 업데이트 시작: 조건={}, 간격={}", condition.getId(), updateInterval);
    }

    /**
     * 이평선 조건 업데이트 (기존 조건 삭제 후 새 조건 생성)
     */
    private Mono<Void> updateMovingAverageCondition(MovingAverageCondition condition) {
        return movingAverageTouchPriceCalculator.calculateTouchPrice(
                condition.getStockCode(), condition.getPeriod(), condition.getInterval())
                .flatMap(newMovingAveragePrice -> {
                    // 기존 PriceCondition 삭제
                    UUID oldConditionId = condition.getCurrentPriceConditionId();
                    if (oldConditionId != null) {
                        boolean removed = monitorPriceService.removePriceCondition(oldConditionId);
                        log.debug("기존 이평선 조건 삭제: 조건ID={}, 성공={}", oldConditionId, removed);
                    }

                    // 새로운 PriceCondition 생성
                    PriceCondition newPriceCondition = condition.createPriceCondition(newMovingAveragePrice);
                    PriceCondition registered = monitorPriceService.registerPriceCondition(newPriceCondition);
                    condition.setCurrentPriceConditionId(registered.getId());

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
        UUID currentPriceConditionId = condition.getCurrentPriceConditionId();
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
}
