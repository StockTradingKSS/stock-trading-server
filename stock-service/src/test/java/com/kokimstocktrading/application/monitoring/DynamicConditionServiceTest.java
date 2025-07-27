package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.application.candle.port.out.LoadStockCandlePort;
import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.candle.StockCandle;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    private MovingAverageTouchPriceCalculator movingAverageTouchPriceCalculator;
    private DynamicConditionService dynamicConditionService;

    @BeforeEach
    void setUp() {
        movingAverageTouchPriceCalculator = new MovingAverageTouchPriceCalculator(loadStockCandlePort);
        dynamicConditionService = new DynamicConditionService(movingAverageTouchPriceCalculator, monitorPriceService);
    }

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
        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(), eq((long) period)))
                .thenReturn(Mono.just(mockCandles));

        // Mock PriceCondition 등록
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //when
        MovingAverageCondition condition = dynamicConditionService
                .registerMovingAverageCondition(stockCode, period, interval, 
                        () -> triggered.compareAndSet(false,true))
                .block();

        //then
        assertThat(condition).isNotNull();
        assertThat(condition.getStockCode()).isEqualTo(stockCode);
        assertThat(condition.getPeriod()).isEqualTo(period);
        assertThat(condition.getInterval()).isEqualTo(interval);
        assertThat(condition.getCurrentPriceConditionId()).isNotNull();
        assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(1);
    }

    @DisplayName("이평선 터치 가격 계산이 정확하게 수행된다.")
    @Test
    public void movingAverageTouchPriceCalculationIsAccurate() {
        //given
        String stockCode = "005930";
        int period = 5;
        CandleInterval interval = CandleInterval.DAY;

        // 명확한 평균값을 가진 캔들 데이터 (70000, 75000, 80000, 85000 / 4 =  80000)
        List<StockCandle> mockCandles = List.of(
                createMockCandle(BigDecimal.valueOf(90000)), // 가장 최신 캔들(현재 캔들)은 수식에 사용되지 않음.
                createMockCandle(BigDecimal.valueOf(87000)),
                createMockCandle(BigDecimal.valueOf(83000)),
                createMockCandle(BigDecimal.valueOf(77000)),
                createMockCandle(BigDecimal.valueOf(73000))  // 가장 오래된
        );

        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(), eq((long) period)))
                .thenReturn(Mono.just(mockCandles));

        //when
        Long movingAverage = movingAverageTouchPriceCalculator
                .calculateTouchPrice(stockCode, period, interval)
                .block();

        //then
        assertThat(movingAverage).isEqualTo(80000L);
    }

    @DisplayName("주기적으로 이평선 조건이 업데이트된다.")
    @Test
    public void movingAverageConditionUpdatedPeriodically() {
        //given
        String stockCode = "005930";
        int period = 20;
        CandleInterval interval = CandleInterval.MINUTE;
        AtomicInteger updateCount = new AtomicInteger(0);

        // 테스트용 빠른 업데이트 간격 설정 (100ms)
        dynamicConditionService.setUpdateIntervalProvider(
                candleInterval -> Duration.ofMillis(100)
        );

        // 첫 번째 호출: 75000원 이평선
        List<StockCandle> initialCandles = createMockCandles(period, BigDecimal.valueOf(75000));
        // 두 번째 호출: 76000원 이평선 (업데이트됨)
        List<StockCandle> updatedCandles = createMockCandles(period, BigDecimal.valueOf(76000));

        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), eq(interval), any(), eq((long) period)))
                .thenReturn(Mono.just(initialCandles))
                .thenReturn(Mono.just(updatedCandles));

        // Mock PriceCondition 등록/삭제
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> {
                    updateCount.incrementAndGet();
                    return invocation.getArgument(0);
                });
        when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

        //when
        MovingAverageCondition condition = dynamicConditionService
                .registerMovingAverageCondition(stockCode, period, interval, () -> {})
                .block();

        //then
        assertThat(condition).isNotNull();
        
        // 초기 등록으로 1번 호출
        assertThat(updateCount.get()).isEqualTo(1);

        // 200ms 후 업데이트 확인 (100ms 간격이므로 충분)
        Awaitility.await()
                .atMost(Duration.ofMillis(500))
                .until(() -> updateCount.get() >= 2);

        assertThat(updateCount.get()).isGreaterThanOrEqualTo(2);
        log.info("이평선 조건 업데이트 횟수: {}", updateCount.get());
    }

    @DisplayName("이평선 조건을 삭제할 수 있다.")
    @Test
    public void canRemoveMovingAverageCondition() {
        //given
        String stockCode = "005930";
        int period = 20;
        CandleInterval interval = CandleInterval.DAY;

        List<StockCandle> mockCandles = createMockCandles(period, BigDecimal.valueOf(75000));
        when(loadStockCandlePort.loadStockCandleListBy(eq(stockCode), any(), any(), eq((long) period)))
                .thenReturn(Mono.just(mockCandles));
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

        MovingAverageCondition condition = dynamicConditionService
                .registerMovingAverageCondition(stockCode, period, interval, () -> {})
                .block();

        assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(1);

        //when
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
        
        when(loadStockCandlePort.loadStockCandleListBy(eq("005930"), any(), any(), eq(20L)))
                .thenReturn(Mono.just(mockCandles20));
        when(loadStockCandlePort.loadStockCandleListBy(eq("000660"), any(), any(), eq(50L)))
                .thenReturn(Mono.just(mockCandles50));
        
        when(monitorPriceService.registerPriceCondition(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(monitorPriceService.removePriceCondition(any())).thenReturn(true);

        // 여러 조건 등록
        dynamicConditionService.registerMovingAverageCondition("005930", 20, CandleInterval.MINUTE, () -> {}).block();
        dynamicConditionService.registerMovingAverageCondition("000660", 50, CandleInterval.DAY, () -> {}).block();

        assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(2);

        //when
        dynamicConditionService.removeAllMovingAverageConditions();

        //then
        assertThat(dynamicConditionService.getMovingAverageConditionCount()).isEqualTo(0);
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
