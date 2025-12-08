package com.kokimstocktrading.application.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kokimstocktrading.application.condition.port.in.RegisterMovingAverageCommand;
import com.kokimstocktrading.application.condition.port.in.RegisterTrendLineCommand;
import com.kokimstocktrading.application.condition.port.out.SaveTradingConditionPort;
import com.kokimstocktrading.application.condition.port.out.TradingTimePort;
import com.kokimstocktrading.application.monitoring.dynamiccondition.DynamicConditionService;
import com.kokimstocktrading.application.notification.port.out.SendNotificationPort;
import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TouchDirection;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

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
        () -> {
        },
        command.description()
    );

    when(saveTradingConditionPort.saveMovingAverageCondition(any(MovingAverageCondition.class)))
        .thenReturn(savedCondition);
    when(tradingTimePort.isTradingTime()).thenReturn(true);
    when(dynamicConditionService.registerMovingAverageCondition(any(MovingAverageCondition.class)))
        .thenReturn(Mono.just(savedCondition));

    // When
    MovingAverageCondition result = tradingConditionService.registerMovingAverageCondition(command);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testConditionId);

    verify(saveTradingConditionPort, times(1)).saveMovingAverageCondition(
        any(MovingAverageCondition.class));
    verify(tradingTimePort, times(1)).isTradingTime();
    verify(dynamicConditionService, times(1)).registerMovingAverageCondition(
        any(MovingAverageCondition.class));
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
        () -> {
        },
        command.description()
    );

    when(saveTradingConditionPort.saveMovingAverageCondition(any(MovingAverageCondition.class)))
        .thenReturn(savedCondition);
    when(tradingTimePort.isTradingTime()).thenReturn(false);

    // When
    MovingAverageCondition result = tradingConditionService.registerMovingAverageCondition(command);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testConditionId);

    verify(saveTradingConditionPort, times(1)).saveMovingAverageCondition(
        any(MovingAverageCondition.class));
    verify(tradingTimePort, times(1)).isTradingTime();
    verify(dynamicConditionService, never()).registerMovingAverageCondition(
        any(MovingAverageCondition.class));
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
        () -> {
        },
        command.description()
    );

    when(saveTradingConditionPort.saveTrendLineCondition(any(TrendLineCondition.class)))
        .thenReturn(savedCondition);
    when(tradingTimePort.isTradingTime()).thenReturn(true);
    when(dynamicConditionService.registerTrendLineCondition(any(TrendLineCondition.class)))
        .thenReturn(Mono.just(savedCondition));

    // When
    TrendLineCondition result = tradingConditionService.registerTrendLineCondition(command);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testConditionId);

    verify(saveTradingConditionPort, times(1)).saveTrendLineCondition(
        any(TrendLineCondition.class));
    verify(tradingTimePort, times(1)).isTradingTime();
    verify(dynamicConditionService, times(1)).registerTrendLineCondition(
        any(TrendLineCondition.class));
  }

  @Test
  @DisplayName("조건 삭제 - 이평선 조건")
  void deleteMovingAverageCondition() {
    // Given
    doNothing().when(saveTradingConditionPort).deleteMovingAverageCondition(testConditionId);
    when(dynamicConditionService.removeMovingAverageCondition(testConditionId)).thenReturn(true);

    // When & Then
    tradingConditionService.deleteCondition(testConditionId);

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

    // When & Then
    tradingConditionService.deleteCondition(testConditionId);

    verify(saveTradingConditionPort, times(1)).deleteMovingAverageCondition(testConditionId);
    verify(saveTradingConditionPort, times(1)).deleteTrendLineCondition(testConditionId);
    verify(dynamicConditionService, times(1)).removeTrendLineCondition(testConditionId);
  }

  @Test
  @DisplayName("조건 삭제 실패 - 존재하지 않는 조건")
  void deleteCondition_notFound() {
    // Given
    doThrow(new RuntimeException("이평선 조건 없음"))
        .when(saveTradingConditionPort).deleteMovingAverageCondition(testConditionId);
    doThrow(new RuntimeException("추세선 조건 없음"))
        .when(saveTradingConditionPort).deleteTrendLineCondition(testConditionId);

    // When & Then
    assertThatThrownBy(() -> tradingConditionService.deleteCondition(testConditionId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("존재하지 않는 조건 ID");
  }
}
