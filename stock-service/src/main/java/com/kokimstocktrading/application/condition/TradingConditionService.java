package com.kokimstocktrading.application.condition;

import com.kokimstocktrading.application.condition.port.in.RegisterMovingAverageCommand;
import com.kokimstocktrading.application.condition.port.in.RegisterTradingConditionUseCase;
import com.kokimstocktrading.application.condition.port.in.RegisterTrendLineCommand;
import com.kokimstocktrading.application.condition.port.out.TradingTimePort;
import com.kokimstocktrading.application.monitoring.DynamicConditionService;
import com.kokimstocktrading.application.notification.port.out.SendNotificationPort;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 거래 조건 관리 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradingConditionService implements RegisterTradingConditionUseCase {

    //    private final SaveTradingConditionPort saveTradingConditionPort;
    private final TradingTimePort tradingTimePort;
    private final DynamicConditionService dynamicConditionService;
    private final SendNotificationPort sendNotificationPort;

    @Override
    @Transactional
    public Mono<MovingAverageCondition> registerMovingAverageCondition(RegisterMovingAverageCommand command) {
        log.info("이평선 조건 등록 요청: 종목={}, 기간={}, 간격={}",
                command.stockCode(), command.period(), command.interval());

        // 1. 도메인 객체 생성
        Mono<MovingAverageCondition> movingAverageConditionMono = dynamicConditionService.registerMovingAverageCondition(
                command.stockCode(),
                command.period(),
                command.interval(),
                command.touchDirection(),
                () -> handleMovingAverageConditionTriggered(command.stockCode(), command.description()),
                command.description()
        );

        movingAverageConditionMono.subscribe(
                condition2 -> log.info("조건 등록 성공: {}", condition2),
                error -> log.error("조건 등록 실패", error)
        );
//        // 2. DB 저장
//        return saveTradingConditionPort.saveMovingAverageCondition(condition)
//                .flatMap(savedCondition -> {
//                    // 3. 거래 시간 체크 후 MonitorPriceService 등록
//                    if (tradingTimePort.isTradingTime()) {
//                        return dynamicConditionService.registerMovingAverageCondition(
//                                savedCondition.getStockCode(),
//                                savedCondition.getPeriod(),
//                                savedCondition.getInterval(),
//                                savedCondition.getCallback(),
//                                savedCondition.getDescription()
//                        ).thenReturn(savedCondition);
//                    } else {
//                        log.info("거래 시간이 아니므로 모니터링 서비스 등록을 건너뜁니다: {}", savedCondition.getId());
//                        return Mono.just(savedCondition);
//                    }
//                })
//                .doOnSuccess(savedCondition ->
//                        log.info("이평선 조건 등록 완료: {}", savedCondition.getId()))
//                .doOnError(error ->
//                        log.error("이평선 조건 등록 실패: {}", command, error));
//
        return null;
    }

    @Override
    @Transactional
    public Mono<TrendLineCondition> registerTrendLineCondition(RegisterTrendLineCommand command) {
        log.info("추세선 조건 등록 요청: 종목={}, 끝점={}, 기울기={}, 간격={}",
                command.stockCode(), command.toDate(), command.slope(), command.interval());

        // 1. 도메인 객체 생성
        TrendLineCondition condition = new TrendLineCondition(
                command.stockCode(), command.toDate(), command.slope(),
                command.interval(),
                command.touchDirection(),
                () -> handleTrendLineConditionTriggered(command.stockCode(), command.description()),
                command.description());

        // 2. DB 저장
//        return saveTradingConditionPort.saveTrendLineCondition(condition)
//                .flatMap(savedCondition -> {
//                    // 3. 거래 시간 체크 후 MonitorPriceService 등록
//                    if (tradingTimePort.isTradingTime()) {
//                        return dynamicConditionService.registerTrendLineCondition(
//                                savedCondition.getStockCode(),
//                                savedCondition.getToDate(),
//                                savedCondition.getSlope(),
//                                savedCondition.getInterval(),
//                                savedCondition.getCallback(),
//                                savedCondition.getDescription()
//                        ).thenReturn(savedCondition);
//                    } else {
//                        log.info("거래 시간이 아니므로 모니터링 서비스 등록을 건너뜁니다: {}", savedCondition.getId());
//                        return Mono.just(savedCondition);
//                    }
//                })
//                .doOnSuccess(savedCondition ->
//                        log.info("추세선 조건 등록 완료: {}", savedCondition.getId()))
//                .doOnError(error ->
//                        log.error("추세선 조건 등록 실패: {}", command, error));
        return null;
    }

    @Override
    @Transactional
    public Mono<Void> deleteCondition(UUID conditionId) {
        log.info("조건 삭제 요청: {}", conditionId);

//        // 이평선 조건 삭제 시도
//        return saveTradingConditionPort.deleteMovingAverageCondition(conditionId)
//                .onErrorResume(error -> {
//                    // 이평선 조건이 없으면 추세선 조건 삭제 시도
//                    return saveTradingConditionPort.deleteTrendLineCondition(conditionId);
//                })
//                .doOnSuccess(unused -> {
//                    // DynamicConditionService에서도 제거 (실제로는 ID 매핑이 필요)
//                    log.info("조건 삭제 완료: {}", conditionId);
//                })
//                .doOnError(error -> log.error("조건 삭제 실패: {}", conditionId, error));
        return null;
    }

    /**
     * 이평선 조건 발동 시 처리
     */
    private void handleMovingAverageConditionTriggered(String stockCode, String description) {
        log.info("이평선 조건 발동: 종목={}, 설명={}", stockCode, description);

        String message = String.format("[이평선 조건 발동]\n종목: %s\n설명: %s", stockCode, description);

        sendNotificationPort.sendKakaoMessage(message)
                .subscribe(
                        unused -> log.info("카카오톡 알림 전송 완료: {}", stockCode),
                        error -> log.error("카카오톡 알림 전송 실패: {}", stockCode, error)
                );
    }

    /**
     * 추세선 조건 발동 시 처리
     */
    private void handleTrendLineConditionTriggered(String stockCode, String description) {
        log.info("추세선 조건 발동: 종목={}, 설명={}", stockCode, description);

        String message = String.format("[추세선 조건 발동]\n종목: %s\n설명: %s", stockCode, description);

        sendNotificationPort.sendKakaoMessage(message)
                .subscribe(
                        unused -> log.info("카카오톡 알림 전송 완료: {}", stockCode),
                        error -> log.error("카카오톡 알림 전송 실패: {}", stockCode, error)
                );
    }

    /**
     * 모든 활성화된 조건을 모니터링 서비스에 등록
     */
    public Mono<Void> registerAllActiveConditions() {
        log.info("모든 활성화된 조건을 모니터링 서비스에 등록 시작");

//        Mono<Void> registerMovingAverages = saveTradingConditionPort.findAllActiveMovingAverageConditions()
//                .flatMap(condition -> dynamicConditionService.registerMovingAverageCondition(
//                        condition.getStockCode(),
//                        condition.getPeriod(),
//                        condition.getInterval(),
//                        condition.getCallback(),
//                        condition.getDescription()
//                ))
//                .then();
//
//        Mono<Void> registerTrendLines = saveTradingConditionPort.findAllActiveTrendLineConditions()
//                .flatMap(condition -> dynamicConditionService.registerTrendLineCondition(
//                        condition.getStockCode(),
//                        condition.getToDate(),
//                        condition.getSlope(),
//                        condition.getInterval(),
//                        condition.getCallback(),
//                        condition.getDescription()
//                ))
//                .then();

//        return Mono.when(registerMovingAverages, registerTrendLines)
//                .doOnSuccess(unused -> log.info("모든 활성화된 조건 등록 완료"))
//                .doOnError(error -> log.error("활성화된 조건 등록 실패", error));
        return null;
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
