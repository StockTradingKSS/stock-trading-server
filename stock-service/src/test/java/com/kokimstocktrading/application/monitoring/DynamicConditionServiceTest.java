package com.kokimstocktrading.application.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kokimstocktrading.application.candle.port.out.LoadStockCandlePort;
import com.kokimstocktrading.application.monitoring.calculator.MovingAverageTouchPriceCalculator;
import com.kokimstocktrading.application.monitoring.calculator.TrendLineTouchPriceCalculator;
import com.kokimstocktrading.application.monitoring.dynamiccondition.DynamicConditionService;
import com.kokimstocktrading.application.monitoring.dynamiccondition.MovingAverageDynamicCondition;
import com.kokimstocktrading.application.monitoring.dynamiccondition.TrendLineDynamicCondition;
import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.candle.StockCandle;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TouchDirection;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DynamicConditionServiceTest {

  @Mock
  private LoadStockCandlePort loadStockCandlePort;

  @Mock
  private MonitorPriceService monitorPriceService;

  @Mock
  private org.springframework.context.ApplicationEventPublisher eventPublisher;

  private TrendLineTouchPriceCalculator trendLineTouchPriceCalculator;
  private MovingAverageTouchPriceCalculator movingAverageTouchPriceCalculator;
  private MovingAverageDynamicCondition movingAverageDynamicCondition;
  private TrendLineDynamicCondition trendLineDynamicCondition;
  private DynamicConditionService dynamicConditionService;

  @BeforeEach
  void setUp() {
    trendLineTouchPriceCalculator = new TrendLineTouchPriceCalculator(loadStockCandlePort);
    movingAverageTouchPriceCalculator = new MovingAverageTouchPriceCalculator(loadStockCandlePort);
    movingAverageDynamicCondition = new MovingAverageDynamicCondition(
        movingAverageTouchPriceCalculator, monitorPriceService, eventPublisher);
    trendLineDynamicCondition = new TrendLineDynamicCondition(trendLineTouchPriceCalculator,
        monitorPriceService, eventPublisher);
    dynamicConditionService = new DynamicConditionService(movingAverageDynamicCondition,
        trendLineDynamicCondition);
  }

  @AfterEach
  void tearDown() {
    // 테스트 후 모든 리소스 정리
    if (dynamicConditionService != null) {
      try {
        dynamicConditionService.removeAllConditions();
        dynamicConditionService.destroy();
      } catch (Exception e) {
        log.error("테스트 정리 중 오류 발생", e);
      }
    }
  }

  // ================================ 이평선 테스트 ================================

  @DisplayName("이평선 조건을 등록할 수 있다.")
  @Test
  public void canRegisterMovingAverageCondition() {
    //given
    String stockCode = "005930";
    int period = 20;
    CandleInterval interval = CandleInterval.MINUTE;
    AtomicBoolean triggered = new AtomicBoolean(false);

    // Mock 캔들 데이터 생성 (20개, 평균 75000원)
    List<StockCandle> mockCandles = createMockCandles(period, BigDecimal.valueOf(75000));
    when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval),
        any(LocalDateTime.class), eq((long) period)))
        .thenReturn(Mono.just(mockCandles));

    // Mock PriceCondition 등록
    when(monitorPriceService.registerPriceCondition(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    //when
    MovingAverageCondition condition = new MovingAverageCondition(
        UUID.randomUUID(), stockCode, period, interval, TouchDirection.FROM_BELOW,
        () -> triggered.compareAndSet(false, true), "설명");
    MovingAverageCondition registeredCondition = dynamicConditionService
        .registerMovingAverageCondition(condition)
        .block();

    //then
    assertThat(registeredCondition).isNotNull();
    assertThat(registeredCondition.getStockCode()).isEqualTo(stockCode);
    assertThat(registeredCondition.getPeriod()).isEqualTo(period);
    assertThat(registeredCondition.getInterval()).isEqualTo(interval);
    assertThat(registeredCondition.getId()).isNotNull();
    assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(1);
  }

  @DisplayName("이평선 터치 가격 계산이 정확하게 수행된다.")
  @Test
  public void movingAverageTouchPriceCalculationIsAccurate() {
    //given
    String stockCode = "005930";
    int period = 5;
    CandleInterval interval = CandleInterval.DAY;

    // 명확한 평균값을 가진 캔들 데이터 (87000, 83000, 77000, 73000 / 4 = 80000)
    List<StockCandle> mockCandles = List.of(
        createMockCandle(BigDecimal.valueOf(90000)), // 가장 최신 캔들(현재 캔들)은 수식에 사용되지 않음.
        createMockCandle(BigDecimal.valueOf(87000)),
        createMockCandle(BigDecimal.valueOf(83000)),
        createMockCandle(BigDecimal.valueOf(77000)),
        createMockCandle(BigDecimal.valueOf(73000))  // 가장 오래된
    );

    when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval),
        any(LocalDateTime.class), eq((long) period)))
        .thenReturn(Mono.just(mockCandles));

    //when
    Long movingAverage = movingAverageTouchPriceCalculator
        .calculateTargetPrice(stockCode, period, interval)
        .block();

    //then
    assertThat(movingAverage).isEqualTo(80000L);
  }

  @DisplayName("1분 간격 이평선 조건은 매분 00초마다 업데이트된다.")
  @Test
  public void movingAverageConditionUpdatedAtEveryMinute() {
    //given
    String stockCode = "005930";
    int period = 20;
    CandleInterval interval = CandleInterval.MINUTE;
    AtomicInteger updateCount = new AtomicInteger(0);

    // 첫 번째 호출: 75000원 이평선
    List<StockCandle> initialCandles = createMockCandles(period, BigDecimal.valueOf(75000));
    // 두 번째 호출: 76000원 이평선 (업데이트됨)
    List<StockCandle> updatedCandles = createMockCandles(period, BigDecimal.valueOf(76000));

    when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval),
        any(LocalDateTime.class), eq((long) period)))
        .thenReturn(Mono.just(initialCandles))
        .thenReturn(Mono.just(updatedCandles))
        .thenReturn(Mono.just(updatedCandles)); // 추가 호출을 위해

    // Mock PriceCondition 등록/삭제
    when(monitorPriceService.registerPriceCondition(any()))
        .thenAnswer(invocation -> {
          updateCount.incrementAndGet();
          log.info("이평선 조건 등록/업데이트 - 횟수: {}, 시간: {}",
              updateCount.get(), LocalDateTime.now());
          return invocation.getArgument(0);
        });
    when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

    // 테스트용: 즉시 실행 후 1초마다 업데이트
    dynamicConditionService.setUpdateIntervalProvider(
        candleInterval -> Duration.ofSeconds(1)
    );
    dynamicConditionService.setInitialDelayProvider(
        duration -> 100L  // 100ms 후 첫 실행
    );

    //when
    MovingAverageCondition condition = new MovingAverageCondition(
        UUID.randomUUID(), stockCode, period, interval, TouchDirection.FROM_BELOW,
        () -> {
        }, "설명");
    MovingAverageCondition registeredCondition = dynamicConditionService
        .registerMovingAverageCondition(condition)
        .block();

    //then
    assertThat(registeredCondition).isNotNull();

    // 초기 등록으로 1번 호출
    assertThat(updateCount.get()).isEqualTo(1);

    // 1초 간격으로 업데이트 확인
    Awaitility.await()
        .atMost(Duration.ofSeconds(3))
        .pollInterval(Duration.ofMillis(100))
        .until(() -> {
          int count = updateCount.get();
          log.info("현재 업데이트 횟수: {}", count);
          return count >= 3;
        });

    assertThat(updateCount.get()).isGreaterThanOrEqualTo(3);
    log.info("이평선 조건 최종 업데이트 횟수: {}", updateCount.get());
  }

  @DisplayName("이평선 조건을 삭제할 수 있다.")
  @Test
  public void canRemoveMovingAverageCondition() {
    //given
    String stockCode = "005930";
    int period = 20;
    CandleInterval interval = CandleInterval.DAY;

    List<StockCandle> mockCandles = createMockCandles(period, BigDecimal.valueOf(75000));
    when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval),
        any(LocalDateTime.class), eq((long) period)))
        .thenReturn(Mono.just(mockCandles));
    when(monitorPriceService.registerPriceCondition(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

    MovingAverageCondition condition = new MovingAverageCondition(
        UUID.randomUUID(), stockCode, period, interval, TouchDirection.FROM_BELOW,
        () -> {
        }, "설명");
    MovingAverageCondition registeredCondition = dynamicConditionService
        .registerMovingAverageCondition(condition)
        .block();

    assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(1);

    //when
    Assertions.assertNotNull(registeredCondition);
    boolean removed = dynamicConditionService.removeMovingAverageCondition(
        registeredCondition.getId());

    //then
    assertThat(removed).isTrue();
    assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(0);
  }

  @DisplayName("모든 이평선 조건을 삭제할 수 있다.")
  @Test
  public void canRemoveAllMovingAverageConditions() {
    //given
    // 20분 이평선용 데이터 (20개)
    List<StockCandle> mockCandles20 = createMockCandles(20, BigDecimal.valueOf(75000));
    // 50일 이평선용 데이터 (50개)
    List<StockCandle> mockCandles50 = createMockCandles(50, BigDecimal.valueOf(85000));

    when(loadStockCandlePort.loadStockCandleListBy(eq("005930"), any(CandleInterval.class),
        any(LocalDateTime.class), eq(20L)))
        .thenReturn(Mono.just(mockCandles20));
    when(loadStockCandlePort.loadStockCandleListBy(eq("000660"), any(CandleInterval.class),
        any(LocalDateTime.class), eq(50L)))
        .thenReturn(Mono.just(mockCandles50));

    when(monitorPriceService.registerPriceCondition(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

    // 여러 조건 등록
    MovingAverageCondition condition1 = new MovingAverageCondition(
        UUID.randomUUID(), "005930", 20, CandleInterval.MINUTE, TouchDirection.FROM_BELOW,
        () -> {
        }, "설명");
    MovingAverageCondition condition2 = new MovingAverageCondition(
        UUID.randomUUID(), "000660", 50, CandleInterval.DAY, TouchDirection.FROM_BELOW,
        () -> {
        }, "설명");
    dynamicConditionService.registerMovingAverageCondition(condition1).block();
    dynamicConditionService.registerMovingAverageCondition(condition2).block();

    assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(2);

    //when
    dynamicConditionService.removeAllMovingAverageConditions();

    //then
    assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(0);
  }

  // ================================ 추세선 테스트 ================================

  @DisplayName("추세선 조건을 등록할 수 있다.")
  @Test
  public void canRegisterTrendLineCondition() {
    //given
    String stockCode = "005930";
    LocalDateTime toDate = LocalDateTime.of(2024, 1, 1, 0, 0);
    BigDecimal slope = BigDecimal.valueOf(100);
    CandleInterval interval = CandleInterval.DAY;
    AtomicBoolean triggered = new AtomicBoolean(false);

    // Mock 캔들 데이터 생성 (5개 봉, 시작가 50000원)
    List<StockCandle> mockCandles = List.of(
        createMockCandle(BigDecimal.valueOf(50000)), // 시작점 (첫 번째 캔들)
        createMockCandle(BigDecimal.valueOf(50500)),
        createMockCandle(BigDecimal.valueOf(51000)),
        createMockCandle(BigDecimal.valueOf(51500)),
        createMockCandle(BigDecimal.valueOf(52000))  // 마지막
    );

    when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval),
        any(LocalDateTime.class), eq(toDate)))
        .thenReturn(Mono.just(mockCandles));

    // Mock PriceCondition 등록
    when(monitorPriceService.registerPriceCondition(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    //when
    TrendLineCondition condition = new TrendLineCondition(
        UUID.randomUUID(), stockCode, toDate, slope, interval, TouchDirection.FROM_ABOVE,
        () -> triggered.compareAndSet(false, true), null);
    TrendLineCondition registeredCondition = dynamicConditionService
        .registerTrendLineCondition(condition)
        .block();

    //then
    assertThat(registeredCondition).isNotNull();
    assertThat(registeredCondition.getStockCode()).isEqualTo(stockCode);
    assertThat(registeredCondition.getToDate()).isEqualTo(toDate);
    assertThat(registeredCondition.getSlope()).isEqualTo(slope);
    assertThat(registeredCondition.getInterval()).isEqualTo(interval);
    assertThat(registeredCondition.getCurrentPriceConditionId()).isNotNull();
    assertThat(dynamicConditionService.getTrendLineConditionCount()).isEqualTo(1);
  }

  @DisplayName("추세선 터치 가격 계산이 정확하게 수행된다.")
  @Test
  public void trendLineTouchPriceCalculationIsAccurate() {
    //given
    String stockCode = "005930";
    LocalDateTime toDate = LocalDateTime.of(2024, 1, 1, 0, 0);
    BigDecimal slope = BigDecimal.valueOf(100); // 봉당 100원씩 상승
    CandleInterval interval = CandleInterval.DAY;

    // 5개 캔들 = 4개 간격, 시작가 50000원 (첫 번째 캔들)
    // 현재가격 = 50000 + (4 * 100) = 50400원
    List<StockCandle> mockCandles = List.of(
        createMockCandle(BigDecimal.valueOf(50000)), // 시작점 (첫 번째 캔들)
        createMockCandle(BigDecimal.valueOf(50500)),
        createMockCandle(BigDecimal.valueOf(51000)),
        createMockCandle(BigDecimal.valueOf(51500)),
        createMockCandle(BigDecimal.valueOf(52000))  // 마지막
    );

    when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval),
        any(LocalDateTime.class), eq(toDate)))
        .thenReturn(Mono.just(mockCandles));

    //when
    Long trendLinePrice = trendLineTouchPriceCalculator
        .calculateTargetPrice(stockCode, toDate, slope, interval)
        .block();

    //then
    // 50000 + (4 * 100) = 50400
    assertThat(trendLinePrice).isEqualTo(50400L);
  }

  @DisplayName("1분 간격 추세선 조건은 매분 00초마다 업데이트된다.")
  @Test
  public void trendLineConditionUpdatedAtEveryMinute() {
    //given
    String stockCode = "005930";
    LocalDateTime toDate = LocalDateTime.of(2024, 1, 1, 0, 0);
    BigDecimal slope = BigDecimal.valueOf(100);
    CandleInterval interval = CandleInterval.MINUTE;
    AtomicInteger updateCount = new AtomicInteger(0);

    // 첫 번째 호출: 50400원 추세선
    List<StockCandle> initialCandles = List.of(
        createMockCandle(BigDecimal.valueOf(50000)), // 시작점
        createMockCandle(BigDecimal.valueOf(50500)),
        createMockCandle(BigDecimal.valueOf(51000)),
        createMockCandle(BigDecimal.valueOf(51500)),
        createMockCandle(BigDecimal.valueOf(52000))
    );

    // 두 번째 호출: 50500원 추세선 (추가 캔들로 인해 변경)
    List<StockCandle> updatedCandles = List.of(
        createMockCandle(BigDecimal.valueOf(50000)), // 시작점
        createMockCandle(BigDecimal.valueOf(50500)),
        createMockCandle(BigDecimal.valueOf(51000)),
        createMockCandle(BigDecimal.valueOf(51500)),
        createMockCandle(BigDecimal.valueOf(52000)),
        createMockCandle(BigDecimal.valueOf(52500))
    );

    when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval),
        any(LocalDateTime.class), eq(toDate)))
        .thenReturn(Mono.just(initialCandles))
        .thenReturn(Mono.just(updatedCandles))
        .thenReturn(Mono.just(updatedCandles)); // 추가 호출을 위해

    // Mock PriceCondition 등록/삭제
    when(monitorPriceService.registerPriceCondition(any()))
        .thenAnswer(invocation -> {
          updateCount.incrementAndGet();
          log.info("추세선 조건 등록/업데이트 - 횟수: {}, 시간: {}",
              updateCount.get(), LocalDateTime.now());
          return invocation.getArgument(0);
        });
    when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

    // 테스트용: 즉시 실행 후 1초마다 업데이트
    dynamicConditionService.setUpdateIntervalProvider(
        candleInterval -> Duration.ofSeconds(1)
    );
    dynamicConditionService.setInitialDelayProvider(
        duration -> 100L  // 100ms 후 첫 실행
    );

    //when
    TrendLineCondition condition = new TrendLineCondition(
        UUID.randomUUID(), stockCode, toDate, slope, interval, TouchDirection.FROM_ABOVE,
        () -> {
        }, null);
    TrendLineCondition registeredCondition = dynamicConditionService
        .registerTrendLineCondition(condition)
        .block();

    //then
    assertThat(registeredCondition).isNotNull();

    // 초기 등록으로 1번 호출
    assertThat(updateCount.get()).isEqualTo(1);

    // 1초 간격으로 업데이트 확인
    Awaitility.await()
        .atMost(Duration.ofSeconds(3))
        .pollInterval(Duration.ofMillis(100))
        .until(() -> {
          int count = updateCount.get();
          log.info("현재 업데이트 횟수: {}", count);
          return count >= 3;
        });

    assertThat(updateCount.get()).isGreaterThanOrEqualTo(3);
    log.info("추세선 조건 최종 업데이트 횟수: {}", updateCount.get());
  }

  @DisplayName("추세선 조건을 삭제할 수 있다.")
  @Test
  public void canRemoveTrendLineCondition() {
    //given
    String stockCode = "005930";
    LocalDateTime toDate = LocalDateTime.of(2024, 1, 1, 0, 0);
    BigDecimal slope = BigDecimal.valueOf(100);
    CandleInterval interval = CandleInterval.DAY;

    List<StockCandle> mockCandles = List.of(
        createMockCandle(BigDecimal.valueOf(50000)), // 시작점
        createMockCandle(BigDecimal.valueOf(50500)),
        createMockCandle(BigDecimal.valueOf(51000)),
        createMockCandle(BigDecimal.valueOf(51500)),
        createMockCandle(BigDecimal.valueOf(52000))
    );

    when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval),
        any(LocalDateTime.class), eq(toDate)))
        .thenReturn(Mono.just(mockCandles));
    when(monitorPriceService.registerPriceCondition(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

    TrendLineCondition condition = new TrendLineCondition(
        UUID.randomUUID(), stockCode, toDate, slope, interval, TouchDirection.FROM_ABOVE,
        () -> {
        }, null);
    TrendLineCondition registeredCondition = dynamicConditionService
        .registerTrendLineCondition(condition)
        .block();

    assertThat(dynamicConditionService.getTrendLineConditionCount()).isEqualTo(1);

    //when
    boolean removed = dynamicConditionService.removeTrendLineCondition(registeredCondition.getId());

    //then
    assertThat(removed).isTrue();
    assertThat(dynamicConditionService.getTrendLineConditionCount()).isEqualTo(0);
  }

  @DisplayName("모든 추세선 조건을 삭제할 수 있다.")
  @Test
  public void canRemoveAllTrendLineConditions() {
    //given
    List<StockCandle> mockCandles = List.of(
        createMockCandle(BigDecimal.valueOf(50000)), // 시작점
        createMockCandle(BigDecimal.valueOf(50500)),
        createMockCandle(BigDecimal.valueOf(51000)),
        createMockCandle(BigDecimal.valueOf(51500)),
        createMockCandle(BigDecimal.valueOf(52000))
    );

    when(loadStockCandlePort.loadStockCandleListBy(anyString(), any(CandleInterval.class),
        any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Mono.just(mockCandles));
    when(monitorPriceService.registerPriceCondition(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

    // 여러 추세선 조건 등록
    TrendLineCondition trendCondition1 = new TrendLineCondition(
        UUID.randomUUID(), "005930", LocalDateTime.of(2024, 1, 1, 0, 0),
        BigDecimal.valueOf(100), CandleInterval.DAY, TouchDirection.FROM_ABOVE, () -> {
    }, null);
    TrendLineCondition trendCondition2 = new TrendLineCondition(
        UUID.randomUUID(), "000660", LocalDateTime.of(2024, 2, 1, 0, 0),
        BigDecimal.valueOf(50), CandleInterval.MINUTE, TouchDirection.FROM_ABOVE, () -> {
    }, null);
    dynamicConditionService.registerTrendLineCondition(trendCondition1).block();
    dynamicConditionService.registerTrendLineCondition(trendCondition2).block();

    assertThat(dynamicConditionService.getTrendLineConditionCount()).isEqualTo(2);

    //when
    dynamicConditionService.removeAllTrendLineConditions();

    //then
    assertThat(dynamicConditionService.getTrendLineConditionCount()).isEqualTo(0);
  }

  @DisplayName("모든 조건(이평선 + 추세선)을 삭제할 수 있다.")
  @Test
  public void canRemoveAllConditions() {
    //given
    List<StockCandle> mockCandles20 = createMockCandles(20, BigDecimal.valueOf(75000));
    List<StockCandle> mockCandles5 = List.of(
        createMockCandle(BigDecimal.valueOf(50000)), // 시작점
        createMockCandle(BigDecimal.valueOf(50500)),
        createMockCandle(BigDecimal.valueOf(51000)),
        createMockCandle(BigDecimal.valueOf(51500)),
        createMockCandle(BigDecimal.valueOf(52000))
    );

    when(loadStockCandlePort.loadStockCandleListBy(eq("005930"), any(CandleInterval.class),
        any(LocalDateTime.class), eq(20L)))
        .thenReturn(Mono.just(mockCandles20));
    when(loadStockCandlePort.loadStockCandleListBy(eq("000660"), any(CandleInterval.class),
        any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Mono.just(mockCandles5));
    when(monitorPriceService.registerPriceCondition(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

    // 이평선 조건 등록
    MovingAverageCondition maCondition = new MovingAverageCondition(
        UUID.randomUUID(), "005930", 20, CandleInterval.DAY, TouchDirection.FROM_BELOW,
        () -> {
        }, "설명");
    dynamicConditionService.registerMovingAverageCondition(maCondition).block();

    // 추세선 조건 등록
    TrendLineCondition tlCondition = new TrendLineCondition(
        UUID.randomUUID(), "000660", LocalDateTime.of(2024, 1, 1, 0, 0),
        BigDecimal.valueOf(100), CandleInterval.DAY, TouchDirection.FROM_ABOVE, () -> {
    }, null);
    dynamicConditionService.registerTrendLineCondition(tlCondition).block();

    assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(1);
    assertThat(dynamicConditionService.getTrendLineConditionCount()).isEqualTo(1);

    //when
    dynamicConditionService.removeAllConditions();

    //then
    assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(0);
    assertThat(dynamicConditionService.getTrendLineConditionCount()).isEqualTo(0);
  }

  // Helper methods
  private List<StockCandle> createMockCandles(int count, BigDecimal averagePrice) {
    return Stream.generate(() -> createMockCandle(averagePrice))
        .limit(count)
        .collect(Collectors.toList());
  }

  private StockCandle createMockCandle(BigDecimal closePrice) {
    return StockCandle.builder()
        .openTime(LocalDateTime.now().minusMinutes(1))
        .openPrice(closePrice.longValue())
        .highPrice(closePrice.add(BigDecimal.valueOf(100)).longValue())
        .lowPrice(closePrice.subtract(BigDecimal.valueOf(100)).longValue())
        .closePrice(closePrice.longValue())
        .volume(1000L)
        .build();
  }
}
