package com.kokimstocktrading.application.condition;

import com.kokimstocktrading.application.condition.port.in.RegisterMovingAverageCommand;
import com.kokimstocktrading.application.condition.port.in.RegisterTrendLineCommand;
import com.kokimstocktrading.application.condition.port.out.SaveTradingConditionPort;
import com.kokimstocktrading.application.condition.port.out.TradingTimePort;
import com.kokimstocktrading.application.monitoring.DynamicConditionService;
import com.kokimstocktrading.application.notification.port.out.SendNotificationPort;
import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TouchDirection;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradingConditionService 테스트")
class TradingConditionServiceTest {

    @Mock
    private SaveTradingConditionPort saveTradingConditionPort;

    @Mock
    private TradingTimePort tradingTimePort;

    @Mock
    private DynamicConditionService dynamicConditionService;

    @Mock
    private SendNotificationPort sendNotificationPort;

    @InjectMocks
    private TradingConditionService tradingConditionService;

    private UUID testConditionId;

    @BeforeEach
    void setUp() {
        testConditionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("거래 시간에 이평선 조건 등록 - DB 저장 및 모니터링 시작")
    void registerMovingAverageCondition_duringTradingTime() {
        // Given
        RegisterMovingAverageCommand command = new RegisterMovingAverageCommand(
                "005930", 20, CandleInterval.DAY, TouchDirection.FROM_BELOW, "삼성전자 20일선 상향 돌파"
        );

        MovingAverageCondition savedCondition = new MovingAverageCondition(
                testConditionId,
                command.stockCode(),
                command.period(),
                command.interval(),
                command.touchDirection(),
                () -> {},
                command.description()
        );

        when(saveTradingConditionPort.saveMovingAverageCondition(any(MovingAverageCondition.class)))
                .thenReturn(savedCondition);
        when(tradingTimePort.isTradingTime()).thenReturn(true);
        when(dynamicConditionService.registerMovingAverageCondition(
                anyString(), anyInt(), any(CandleInterval.class), any(TouchDirection.class), any(Runnable.class), anyString()
        )).thenReturn(Mono.just(savedCondition));

        // When
        Mono<MovingAverageCondition> result = tradingConditionService.registerMovingAverageCondition(command);

        // Then
        StepVerifier.create(result)
                .expectNext(savedCondition)
                .verifyComplete();

        verify(saveTradingConditionPort, times(1)).saveMovingAverageCondition(any(MovingAverageCondition.class));
        verify(tradingTimePort, times(1)).isTradingTime();
        verify(dynamicConditionService, times(1)).registerMovingAverageCondition(
                anyString(), anyInt(), any(CandleInterval.class), any(TouchDirection.class), any(Runnable.class), anyString()
        );
    }

    @Test
    @DisplayName("거래 시간 외 이평선 조건 등록 - DB만 저장, 모니터링 생략")
    void registerMovingAverageCondition_outsideTradingTime() {
        // Given
        RegisterMovingAverageCommand command = new RegisterMovingAverageCommand(
                "005930", 20, CandleInterval.DAY, TouchDirection.FROM_BELOW, "삼성전자 20일선 상향 돌파"
        );

        MovingAverageCondition savedCondition = new MovingAverageCondition(
                testConditionId,
                command.stockCode(),
                command.period(),
                command.interval(),
                command.touchDirection(),
                () -> {},
                command.description()
        );

        when(saveTradingConditionPort.saveMovingAverageCondition(any(MovingAverageCondition.class)))
                .thenReturn(savedCondition);
        when(tradingTimePort.isTradingTime()).thenReturn(false);

        // When
        Mono<MovingAverageCondition> result = tradingConditionService.registerMovingAverageCondition(command);

        // Then
        StepVerifier.create(result)
                .expectNext(savedCondition)
                .verifyComplete();

        verify(saveTradingConditionPort, times(1)).saveMovingAverageCondition(any(MovingAverageCondition.class));
        verify(tradingTimePort, times(1)).isTradingTime();
        verify(dynamicConditionService, never()).registerMovingAverageCondition(
                anyString(), anyInt(), any(CandleInterval.class), any(TouchDirection.class), any(Runnable.class), anyString()
        );
    }

    @Test
    @DisplayName("거래 시간에 추세선 조건 등록 - DB 저장 및 모니터링 시작")
    void registerTrendLineCondition_duringTradingTime() {
        // Given
        RegisterTrendLineCommand command = new RegisterTrendLineCommand(
                "005930",
                LocalDateTime.now(),
                new BigDecimal("0.5"),
                TouchDirection.FROM_ABOVE,
                CandleInterval.DAY,
                "삼성전자 추세선 하향 돌파"
        );

        TrendLineCondition savedCondition = new TrendLineCondition(
                testConditionId,
                command.stockCode(),
                command.toDate(),
                command.slope(),
                command.interval(),
                command.touchDirection(),
                () -> {},
                command.description()
        );

        when(saveTradingConditionPort.saveTrendLineCondition(any(TrendLineCondition.class)))
                .thenReturn(savedCondition);
        when(tradingTimePort.isTradingTime()).thenReturn(true);
        when(dynamicConditionService.registerTrendLineCondition(
                anyString(), any(LocalDateTime.class), any(BigDecimal.class), any(CandleInterval.class),
                any(TouchDirection.class), any(Runnable.class), anyString()
        )).thenReturn(Mono.just(savedCondition));

        // When
        Mono<TrendLineCondition> result = tradingConditionService.registerTrendLineCondition(command);

        // Then
        StepVerifier.create(result)
                .expectNext(savedCondition)
                .verifyComplete();

        verify(saveTradingConditionPort, times(1)).saveTrendLineCondition(any(TrendLineCondition.class));
        verify(tradingTimePort, times(1)).isTradingTime();
        verify(dynamicConditionService, times(1)).registerTrendLineCondition(
                anyString(), any(LocalDateTime.class), any(BigDecimal.class), any(CandleInterval.class),
                any(TouchDirection.class), any(Runnable.class), anyString()
        );
    }

    @Test
    @DisplayName("조건 삭제 - 이평선 조건")
    void deleteMovingAverageCondition() {
        // Given
        doNothing().when(saveTradingConditionPort).deleteMovingAverageCondition(testConditionId);
        when(dynamicConditionService.removeMovingAverageCondition(testConditionId)).thenReturn(true);

        // When
        Mono<Void> result = tradingConditionService.deleteCondition(testConditionId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(saveTradingConditionPort, times(1)).deleteMovingAverageCondition(testConditionId);
        verify(dynamicConditionService, times(1)).removeMovingAverageCondition(testConditionId);
    }

    @Test
    @DisplayName("조건 삭제 - 추세선 조건")
    void deleteTrendLineCondition() {
        // Given
        doThrow(new RuntimeException("이평선 조건 없음"))
                .when(saveTradingConditionPort).deleteMovingAverageCondition(testConditionId);
        doNothing().when(saveTradingConditionPort).deleteTrendLineCondition(testConditionId);
        when(dynamicConditionService.removeTrendLineCondition(testConditionId)).thenReturn(true);

        // When
        Mono<Void> result = tradingConditionService.deleteCondition(testConditionId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(saveTradingConditionPort, times(1)).deleteMovingAverageCondition(testConditionId);
        verify(saveTradingConditionPort, times(1)).deleteTrendLineCondition(testConditionId);
        verify(dynamicConditionService, times(1)).removeTrendLineCondition(testConditionId);
    }

    @Test
    @DisplayName("모든 활성화된 조건 등록 - 앱 시작 시")
    void registerAllActiveConditions() {
        // Given
        MovingAverageCondition maCondition = new MovingAverageCondition(
                UUID.randomUUID(),
                "005930",
                20,
                CandleInterval.DAY,
                TouchDirection.FROM_BELOW,
                () -> {},
                "삼성전자 20일선"
        );

        TrendLineCondition tlCondition = new TrendLineCondition(
                UUID.randomUUID(),
                "005930",
                LocalDateTime.now(),
                new BigDecimal("0.5"),
                CandleInterval.DAY,
                TouchDirection.FROM_ABOVE,
                () -> {},
                "삼성전자 추세선"
        );

        when(saveTradingConditionPort.findAllActiveMovingAverageConditions())
                .thenReturn(List.of(maCondition));
        when(saveTradingConditionPort.findAllActiveTrendLineConditions())
                .thenReturn(List.of(tlCondition));
        when(dynamicConditionService.registerMovingAverageCondition(
                anyString(), anyInt(), any(CandleInterval.class), any(TouchDirection.class), any(Runnable.class), anyString()
        )).thenReturn(Mono.just(maCondition));
        when(dynamicConditionService.registerTrendLineCondition(
                anyString(), any(LocalDateTime.class), any(BigDecimal.class), any(CandleInterval.class),
                any(TouchDirection.class), any(Runnable.class), anyString()
        )).thenReturn(Mono.just(tlCondition));

        // When
        Mono<Void> result = tradingConditionService.registerAllActiveConditions();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(saveTradingConditionPort, times(1)).findAllActiveMovingAverageConditions();
        verify(saveTradingConditionPort, times(1)).findAllActiveTrendLineConditions();
        verify(dynamicConditionService, times(1)).registerMovingAverageCondition(
                anyString(), anyInt(), any(CandleInterval.class), any(TouchDirection.class), any(Runnable.class), anyString()
        );
        verify(dynamicConditionService, times(1)).registerTrendLineCondition(
                anyString(), any(LocalDateTime.class), any(BigDecimal.class), any(CandleInterval.class),
                any(TouchDirection.class), any(Runnable.class), anyString()
        );
    }
}
