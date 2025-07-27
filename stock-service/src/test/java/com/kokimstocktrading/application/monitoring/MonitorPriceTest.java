package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.application.monitoring.mock.RealTimeQuotePortConfig;
import com.kokimstocktrading.application.realtime.out.SubscribeRealTimeQuotePort;
import com.kokimstocktrading.domain.realtime.RealTimeQuote;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(RealTimeQuotePortConfig.class)
@Slf4j
public class MonitorPriceTest {

    @Autowired
    private MonitorPriceService monitorPriceService;

    /**
     * resources/stock-price-scenarios.txt 에 차트 시나리오가 있습니다.
     */
    @Autowired
    private SubscribeRealTimeQuotePort subscribeRealTimeQuotePort;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 모니터링 중지
        monitorPriceService.stopAllMonitoring();
    }

    @DisplayName("삼성전자가 76000원에 도달하면 조건 감시가 동작한다.")
    @Test
    public void detectSamsungPriceCondition() {
        //given
        String stockCode = "005930";
        Long targetPrice = 76000L;
        AtomicBoolean conditionTriggered = new AtomicBoolean(false);

        //when
        monitorPriceService.registerPriceCondition(stockCode, targetPrice,
                () -> {
                    System.out.println("조건감시 - 삼성전자 76000원 달성!");
                    conditionTriggered.set(true);
                });

        monitorPriceService.startMonitoring();

        //then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilTrue(conditionTriggered);
    }

    @DisplayName("SK하이닉스가 86000원에 도달하면 조건 감시가 동작한다.")
    @Test
    public void detectSKHynixPriceCondition() {
        //given
        String stockCode = "000660";
        Long targetPrice = 86000L;
        AtomicBoolean conditionTriggered = new AtomicBoolean(false);

        //when
        monitorPriceService.registerPriceCondition(stockCode, targetPrice,
                () -> {
                    System.out.println("조건감시 - SK하이닉스 86000원 달성!");
                    conditionTriggered.set(true);
                });

        monitorPriceService.startMonitoring();

        //then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilTrue(conditionTriggered);
    }

    @DisplayName("여러 종목에 대해 각각 다른 가격 조건을 동시에 모니터링할 수 있다.")
    @Test
    public void canMonitorMultipleStocksSimultaneously() {
        //given
        AtomicInteger triggeredCount = new AtomicInteger(0);

        //when
        monitorPriceService.registerPriceCondition("005930", 76000L,
                () -> {
                    System.out.println("조건감시 - 삼성전자 76000원 달성!");
                    triggeredCount.incrementAndGet();
                });

        monitorPriceService.registerPriceCondition("000660", 86000L,
                () -> {
                    System.out.println("조건감시 - SK하이닉스 86000원 달성!");
                    triggeredCount.incrementAndGet();
                });

        monitorPriceService.registerPriceCondition("035420", 860000L,
                () -> {
                    System.out.println("조건감시 - NAVER 860000원 달성!");
                    triggeredCount.incrementAndGet();
                });

        monitorPriceService.startMonitoring();

        //then
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> triggeredCount.get() > 0);
        
        System.out.println("총 " + triggeredCount.get() + "개 조건이 달성되었습니다.");
    }

    @DisplayName("개별 종목 모니터링이 정상 동작한다.")
    @Test
    public void canMonitorIndividualStock() {
        //given
        String stockCode = "005930";
        Long targetPrice = 76000L;
        AtomicBoolean conditionTriggered = new AtomicBoolean(false);

        //when
        monitorPriceService.registerPriceCondition(stockCode, targetPrice,
                () -> {
                    System.out.println("조건감시 - 개별 모니터링 삼성전자 76000원 달성!");
                    conditionTriggered.set(true);
                });

        // 개별 종목 모니터링 시작
        monitorPriceService.startMonitoring(stockCode);

        //then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilTrue(conditionTriggered);
    }

    @DisplayName("조건 달성 후 해당 종목 모니터링이 자동으로 중지된다.")
    @Test
    public void stopMonitoringAfterConditionMet() {
        //given
        String stockCode = "005930";
        Long targetPrice = 76000L;
        AtomicInteger triggerCount = new AtomicInteger(0);

        //when
        monitorPriceService.registerPriceCondition(stockCode, targetPrice,
                () -> {
                    System.out.println("조건감시 - 중지 테스트");
                    triggerCount.incrementAndGet();
                });

        monitorPriceService.startMonitoring();

        //then
        // 첫 번째 조건 달성까지 기다림
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> triggerCount.get() >= 1);

        // 추가로 2초 더 기다려서 중복 트리거 안되는지 확인
        Awaitility.await()
                .during(Duration.ofSeconds(2))
                .until(() -> triggerCount.get() == 1);

        System.out.println("조건 트리거 횟수: " + triggerCount.get());
        assertThat(triggerCount.get()).isEqualTo(1);
    }

    @DisplayName("Mock 어댑터를 통해 실시간 시세 데이터를 수신할 수 있다.")
    @Test
    public void canReceiveRealTimeQuoteFromMockAdapter() {
        //given
        String stockCode = "005930";

        //when
        Flux<RealTimeQuote> quoteFlux = subscribeRealTimeQuotePort.subscribeStockQuote(List.of(stockCode));

        //then
        StepVerifier.create(quoteFlux.take(3).timeout(Duration.ofSeconds(10)))
                .assertNext(quote -> {
                    assertThat(quote.item()).isEqualTo(stockCode);
                    assertThat(quote.name()).isEqualTo("삼성전자");
                    assertThat(quote.currentPrice()).isNotBlank();
                    System.out.println("수신된 시세: " + quote);
                })
                .assertNext(quote -> {
                    assertThat(quote.item()).isEqualTo(stockCode);
                    System.out.println("수신된 시세: " + quote);
                })
                .assertNext(quote -> {
                    assertThat(quote.item()).isEqualTo(stockCode);
                    System.out.println("수신된 시세: " + quote);
                })
                .verifyComplete();
    }
}
