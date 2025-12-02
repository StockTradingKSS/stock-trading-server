package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.application.candle.port.out.LoadStockCandlePort;
import com.kokimstocktrading.application.monitoring.calculator.MovingAverageTouchPriceCalculator;
import com.kokimstocktrading.application.monitoring.calculator.TrendLineTouchPriceCalculator;
import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.candle.StockCandle;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TouchDirection;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DynamicConditionServiceTest {

    @Mock
    private LoadStockCandlePort loadStockCandlePort;

    @Mock
    private MonitorPriceService monitorPriceService;

    private TrendLineTouchPriceCalculator trendLineTouchPriceCalculator;
    private MovingAverageTouchPriceCalculator movingAverageTouchPriceCalculator;
    private DynamicConditionService dynamicConditionService;

    @BeforeEach
    void setUp() {
        trendLineTouchPriceCalculator = new TrendLineTouchPriceCalculator(loadStockCandlePort);
        movingAverageTouchPriceCalculator = new MovingAverageTouchPriceCalculator(loadStockCandlePort);
        dynamicConditionService = new DynamicConditionService(movingAverageTouchPriceCalculator, trendLineTouchPriceCalculator, monitorPriceService);
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
        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(LocalDateTime.class), eq((long) period)))
                .thenReturn(Mono.just(mockCandles));

        // Mock PriceCondition 등록
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //when
        MovingAverageCondition condition = dynamicConditionService
                .registerMovingAverageCondition(stockCode, period, interval, TouchDirection.FROM_BELOW,
                        () -> triggered.compareAndSet(false,true), "설명")
                .block();

        //then
        assertThat(condition).isNotNull();
        assertThat(condition.getStockCode()).isEqualTo(stockCode);
        assertThat(condition.getPeriod()).isEqualTo(period);
        assertThat(condition.getInterval()).isEqualTo(interval);
        assertThat(condition.getId()).isNotNull();
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

        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(LocalDateTime.class), eq((long) period)))
                .thenReturn(Mono.just(mockCandles));

        //when
        Long movingAverage = movingAverageTouchPriceCalculator
                .calculateTouchPrice(stockCode, period, interval)
                .block();

        //then
        assertThat(movingAverage).isEqualTo(80000L);
    }

    @DisplayName("[문제 재현] 기존 로직: subscription이 누적되어 여러 번 업데이트된다")
    @Test
    public void oldLogic_subscriptionAccumulation_causeMultipleUpdates() {
        //given
        AtomicInteger updateCount = new AtomicInteger(0);
        AtomicInteger activeSubscriptionCount = new AtomicInteger(0);

        log.info("=== 기존 로직 테스트 시작 ===");

        // 기존 문제 로직 재현: defer + Mono.fromRunnable(subscribe()) + repeat
        // ⚠️ repeat(5)로 제한해서 안전하게 테스트
        Disposable problematicScheduler = Flux.defer(() -> {
                    log.info("defer() 실행 - 새로운 Mono.fromRunnable 생성");
                    return Mono.fromRunnable(() -> {
                        // 여기서 비동기 작업을 subscribe()로 실행 (문제!)
                        Mono.delay(Duration.ofMillis(10))
                            .doOnSubscribe(s -> {
                                int count = activeSubscriptionCount.incrementAndGet();
                                log.warn("🔥 새로운 subscription 생성! 현재 활성 subscription: {}", count);
                            })
                            .doOnNext(tick -> {
                                int count = updateCount.incrementAndGet();
                                log.info("업데이트 실행 #{} (활성 subscription: {})",
                                    count, activeSubscriptionCount.get());
                            })
                            .subscribe();  // ❌ 문제: 여기서 subscription 생성!
                    });
                })
                .take(5)  // ⚠️ 안전하게 5개로 제한 (repeat() 대신)
                .subscribe();

        //when - 100ms만 대기 (즉시 5개 생성될 것임)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        problematicScheduler.dispose();

        //then
        log.info("=== 최종 결과 ===");
        log.info("총 subscription 생성 수: {}", activeSubscriptionCount.get());
        log.info("총 업데이트 실행 횟수: {}", updateCount.get());

        // Mono.fromRunnable은 즉시 완료되므로 take(5)면 거의 즉시 5개 생성
        assertThat(activeSubscriptionCount.get())
            .describedAs("Mono.fromRunnable이 즉시 완료되어 5개 subscription이 빠르게 생성됨")
            .isEqualTo(5);

        // 모든 subscription이 살아있어서 각각 업데이트 실행
        assertThat(updateCount.get())
            .describedAs("누적된 subscription들이 각각 실행")
            .isGreaterThanOrEqualTo(5);

        log.info("✅ 기존 로직의 문제 확인: Mono.fromRunnable이 즉시 완료되어 subscription 폭발적 증가!");
    }

    @DisplayName("[해결] 새로운 로직: Flux.interval로 단일 subscription만 유지된다")
    @Test
    public void newLogic_singleSubscription_updatesCorrectly() {
        //given
        AtomicInteger updateCount = new AtomicInteger(0);
        AtomicInteger subscriptionCount = new AtomicInteger(0);

        log.info("=== 새로운 로직 테스트 시작 ===");

        // 수정된 로직: Flux.interval 사용
        Disposable correctScheduler = Flux.interval(
                Duration.ofMillis(100),  // 초기 지연
                Duration.ofMillis(100)   // 주기
            )
            .doOnSubscribe(s -> {
                int count = subscriptionCount.incrementAndGet();
                log.info("✅ Flux.interval subscription 생성: {}", count);
            })
            .take(5)  // 5번만 실행
            .flatMap(tick ->
                Mono.delay(Duration.ofMillis(10))
                    .doOnNext(t -> {
                        int count = updateCount.incrementAndGet();
                        log.info("업데이트 실행 #{}", count);
                    })
            )
            .subscribe();

        //when - 1초 대기
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        correctScheduler.dispose();

        //then
        log.info("=== 최종 결과 ===");
        log.info("총 subscription 생성 수: {}", subscriptionCount.get());
        log.info("총 업데이트 실행 횟수: {}", updateCount.get());

        // Flux.interval은 단일 subscription만 생성
        assertThat(subscriptionCount.get())
            .describedAs("Flux.interval은 단 1개의 subscription만 생성")
            .isEqualTo(1);

        // 정확히 5번만 업데이트
        assertThat(updateCount.get())
            .describedAs("정확히 5번만 업데이트 실행")
            .isEqualTo(5);

        log.info("✅ 새로운 로직 확인: 단일 subscription으로 정확한 횟수만큼 업데이트");
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

        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(LocalDateTime.class), eq((long) period)))
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
        MovingAverageCondition condition = dynamicConditionService
                .registerMovingAverageCondition(stockCode, period, interval, TouchDirection.FROM_BELOW,() -> {}, "설명")
                .block();

        //then
        assertThat(condition).isNotNull();
        
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
        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(LocalDateTime.class), eq((long) period)))
                .thenReturn(Mono.just(mockCandles));
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

        MovingAverageCondition condition = dynamicConditionService
                .registerMovingAverageCondition(stockCode, period, interval, TouchDirection.FROM_BELOW ,() -> {}, "설명")
                .block();

        assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(1);

        //when
        Assertions.assertNotNull(condition);
        boolean removed = dynamicConditionService.removeMovingAverageCondition(condition.getId());

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
        
        when(loadStockCandlePort.loadStockCandleListBy(eq("005930"), any(CandleInterval.class), any(LocalDateTime.class), eq(20L)))
                .thenReturn(Mono.just(mockCandles20));
        when(loadStockCandlePort.loadStockCandleListBy(eq("000660"), any(CandleInterval.class), any(LocalDateTime.class), eq(50L)))
                .thenReturn(Mono.just(mockCandles50));
        
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

        // 여러 조건 등록
        dynamicConditionService.registerMovingAverageCondition("005930", 20, CandleInterval.MINUTE, TouchDirection.FROM_BELOW,() -> {}, "설명").block();
        dynamicConditionService.registerMovingAverageCondition("000660", 50, CandleInterval.DAY, TouchDirection.FROM_BELOW,() -> {}, "설명").block();

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

        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(LocalDateTime.class), eq(toDate)))
                .thenReturn(Mono.just(mockCandles));

        // Mock PriceCondition 등록
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //when
        TrendLineCondition condition = dynamicConditionService
                .registerTrendLineCondition(stockCode, toDate, slope, interval, 
                        () -> triggered.compareAndSet(false, true))
                .block();

        //then
        assertThat(condition).isNotNull();
        assertThat(condition.getStockCode()).isEqualTo(stockCode);
        assertThat(condition.getToDate()).isEqualTo(toDate);
        assertThat(condition.getSlope()).isEqualTo(slope);
        assertThat(condition.getInterval()).isEqualTo(interval);
        assertThat(condition.getCurrentPriceConditionId()).isNotNull();
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

        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(LocalDateTime.class), eq(toDate)))
                .thenReturn(Mono.just(mockCandles));

        //when
        Long trendLinePrice = trendLineTouchPriceCalculator
                .calculateTouchPrice(stockCode, toDate, slope, interval)
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

        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(LocalDateTime.class), eq(toDate)))
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
        TrendLineCondition condition = dynamicConditionService
                .registerTrendLineCondition(stockCode, toDate, slope, interval, () -> {})
                .block();

        //then
        assertThat(condition).isNotNull();
        
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

        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(LocalDateTime.class), eq(toDate)))
                .thenReturn(Mono.just(mockCandles));
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

        TrendLineCondition condition = dynamicConditionService
                .registerTrendLineCondition(stockCode, toDate, slope, interval, () -> {})
                .block();

        assertThat(dynamicConditionService.getTrendLineConditionCount()).isEqualTo(1);

        //when
        boolean removed = dynamicConditionService.removeTrendLineCondition(condition.getId());

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
        
        when(loadStockCandlePort.loadStockCandleListBy(anyString(), any(CandleInterval.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Mono.just(mockCandles));
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

        // 여러 추세선 조건 등록
        dynamicConditionService.registerTrendLineCondition(
                "005930", LocalDateTime.of(2024, 1, 1, 0, 0), 
                BigDecimal.valueOf(100), CandleInterval.DAY, () -> {}).block();
        dynamicConditionService.registerTrendLineCondition(
                "000660", LocalDateTime.of(2024, 2, 1, 0, 0), 
                BigDecimal.valueOf(50), CandleInterval.MINUTE, () -> {}).block();

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
        
        when(loadStockCandlePort.loadStockCandleListBy(eq("005930"), any(CandleInterval.class), any(LocalDateTime.class), eq(20L)))
                .thenReturn(Mono.just(mockCandles20));
        when(loadStockCandlePort.loadStockCandleListBy(eq("000660"), any(CandleInterval.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Mono.just(mockCandles5));
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

        // 이평선 조건 등록
        dynamicConditionService.registerMovingAverageCondition("005930", 20, CandleInterval.DAY, TouchDirection.FROM_BELOW, () -> {}, "설명").block();
        
        // 추세선 조건 등록
        dynamicConditionService.registerTrendLineCondition(
                "000660", LocalDateTime.of(2024, 1, 1, 0, 0), 
                BigDecimal.valueOf(100), CandleInterval.DAY, () -> {}).block();

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
