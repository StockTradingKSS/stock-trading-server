package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.application.monitoring.mock.RealTimeQuotePortConfig;
import com.kokimstocktrading.application.realtime.out.SubscribeRealTimeQuotePort;
import com.kokimstocktrading.domain.monitoring.PriceCondition;
import com.kokimstocktrading.domain.monitoring.TouchDirection;
import com.kokimstocktrading.domain.realtime.RealTimeQuote;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RealTimeQuotePortConfig.class)
@Slf4j
public class MonitorPriceTest {

    /**
     *   종목 코드 : "005930", 시작 가격 : "75000", 종목 이름 : 삼성전자
     *   종목 코드 : "000660", 시작 가격 : "85000", 종목 이름 : SK하이닉스
     *   종목 코드 : "035420", 시작 가격 : "850000", 종목 이름 : NAVER
     *   종목 코드 : "051910", 시작 가격 : "4500" , 종목 이름 : LG화학
     *   종목 코드 : "006400", 시작 가격 : "35000", 종목 이름 : 삼성SDI
     *   다음 시세의 변동폭은 전 시세의 +- 5 % 입니다.
     */

    @Autowired
    private MonitorPriceService monitorPriceService;

    @Autowired
    private SubscribeRealTimeQuotePort subscribeRealTimeQuotePort;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 모니터링 중지
        monitorPriceService.stopAllMonitoring();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후에도 모니터링 중지 (이미 모든 조건도 함께 제거됨)
        monitorPriceService.stopAllMonitoring();
    }

    @DisplayName("가격 조건 도메인 모델을 등록할 수 있다.")
    @Test
    public void canRegisterPriceConditionModel() {
        //given
        AtomicBoolean triggered = new AtomicBoolean(false);
        PriceCondition condition = new PriceCondition(UUID.randomUUID(),"005930", 76000L, TouchDirection.FROM_ABOVE,
                () -> triggered.set(true), "삼성전자 목표가 달성");

        //when
        PriceCondition registered = monitorPriceService.registerPriceCondition(condition);

        //then
        assertThat(registered).isNotNull();
        assertThat(registered.getId()).isNotNull();
        assertThat(registered.getId()).isInstanceOf(UUID.class);
        assertThat(registered.getStockCode()).isEqualTo("005930");
        assertThat(registered.getTargetPrice()).isEqualTo(76000L);
        assertThat(registered.getDescription()).isEqualTo("삼성전자 목표가 달성");
        assertThat(monitorPriceService.getTotalConditionCount()).isEqualTo(1);
    }

    @DisplayName("조건 ID로 조건을 삭제할 수 있다.")
    @Test
    public void canRemoveConditionById() {
        //given
        PriceCondition condition1 = new PriceCondition(UUID.randomUUID(), "005930", 76000L, () -> {}, TouchDirection.FROM_BELOW);
        PriceCondition condition2 = new PriceCondition(UUID.randomUUID(), "005930", 77000L, () -> {}, TouchDirection.FROM_BELOW);

        PriceCondition registered1 = monitorPriceService.registerPriceCondition(condition1);
        PriceCondition registered2 = monitorPriceService.registerPriceCondition(condition2);

        assertThat(monitorPriceService.getTotalConditionCount()).isEqualTo(2);

        //when
        boolean removed = monitorPriceService.removePriceCondition(registered1.getId());

        //then
        assertThat(removed).isTrue();
        assertThat(monitorPriceService.getTotalConditionCount()).isEqualTo(1);
        assertThat(monitorPriceService.getCondition(registered1.getId())).isEmpty();
        assertThat(monitorPriceService.getCondition(registered2.getId())).isPresent();
    }

    @DisplayName("조건 ID로 특정 조건을 조회할 수 있다.")
    @Test
    public void canGetConditionById() {
        //given
        PriceCondition condition = new PriceCondition(UUID.randomUUID(), "005930", 76000L, TouchDirection.FROM_ABOVE, () -> {}, "테스트 조건");
        PriceCondition registered = monitorPriceService.registerPriceCondition(condition);

        //when
        Optional<PriceCondition> found = monitorPriceService.getCondition(registered.getId());

        //then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(registered.getId());
        assertThat(found.get().getStockCode()).isEqualTo("005930");
        assertThat(found.get().getTargetPrice()).isEqualTo(76000L);
        assertThat(found.get().getDescription()).isEqualTo("테스트 조건");
    }

    @DisplayName("삼성전자가 76000원에 도달하면 조건 감시가 동작한다.")
    @Test
    public void detectSamsungPriceCondition() {
        //given
        AtomicBoolean conditionTriggered = new AtomicBoolean(false);
        PriceCondition condition = new PriceCondition(UUID.randomUUID(), "005930", 76000L,
                () -> {
                    System.out.println("조건감시 - 삼성전자 76000원 달성!");
                    conditionTriggered.set(true);
                }, TouchDirection.FROM_BELOW);

        //when
        monitorPriceService.registerPriceCondition(condition);
        monitorPriceService.startMonitoring();

        //then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilTrue(conditionTriggered);
    }

    @DisplayName("하나의 종목에 여러 가격 조건을 등록할 수 있다.")
    @Test
    public void canRegisterMultipleConditionsForSingleStock() {
        //given
        String stockCode = "005930";
        AtomicInteger conditionCount = new AtomicInteger(0);

        //when
        PriceCondition condition1 = new PriceCondition(UUID.randomUUID(), stockCode, 76000L,
                () -> {
                    System.out.println("조건1 달성 - 삼성전자 76000원!");
                    conditionCount.incrementAndGet();
                }, TouchDirection.FROM_BELOW);

        PriceCondition condition2 = new PriceCondition(UUID.randomUUID(), stockCode, 77000L,
                () -> {
                    System.out.println("조건2 달성 - 삼성전자 77000원!");
                    conditionCount.incrementAndGet();
                }, TouchDirection.FROM_BELOW);

        PriceCondition condition3 = new PriceCondition(UUID.randomUUID(), stockCode, 78000L,
                () -> {
                    System.out.println("조건3 달성 - 삼성전자 78000원!");
                    conditionCount.incrementAndGet();
                },TouchDirection.FROM_BELOW);

        PriceCondition registered1 = monitorPriceService.registerPriceCondition(condition1);
        PriceCondition registered2 = monitorPriceService.registerPriceCondition(condition2);
        PriceCondition registered3 = monitorPriceService.registerPriceCondition(condition3);

        //then
        assertThat(monitorPriceService.getConditionCount(stockCode)).isEqualTo(3);
        assertThat(registered1.getId()).isNotNull();
        assertThat(registered2.getId()).isNotNull();
        assertThat(registered3.getId()).isNotNull();
        assertThat(registered1.getId()).isNotEqualTo(registered2.getId());
        assertThat(registered2.getId()).isNotEqualTo(registered3.getId());
    }

    @DisplayName("조건 달성 후 해당 조건이 자동으로 삭제된다.")
    @Test
    public void conditionRemovedAfterAchievement() {
        //given
        AtomicBoolean triggered = new AtomicBoolean(false);
        PriceCondition condition = new PriceCondition(UUID.randomUUID(), "005930", 74000L, // 시작가보다 낮은 조건 (즉시 달성)
                () -> triggered.set(true), TouchDirection.FROM_BELOW);

        //when
        PriceCondition registered = monitorPriceService.registerPriceCondition(condition);
        assertThat(monitorPriceService.getCondition(registered.getId())).isPresent();

        monitorPriceService.startMonitoring();

        //then
        // 조건 달성 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilTrue(triggered);

        // 조건이 자동으로 삭제되었는지 확인
        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .until(() -> monitorPriceService.getCondition(registered.getId()).isEmpty());

        assertThat(monitorPriceService.getCondition(registered.getId())).isEmpty();
        System.out.println("조건 달성 후 자동 삭제 확인");
    }

    @DisplayName("특정 조건을 수동으로 삭제할 수 있다.")
    @Test
    public void canManuallyRemoveSpecificCondition() {
        //given
        String stockCode = "005930";

        //when
        PriceCondition condition1 = new PriceCondition(UUID.randomUUID(), stockCode, 76000L, () -> {}, TouchDirection.FROM_BELOW);
        PriceCondition condition2 = new PriceCondition(UUID.randomUUID() ,stockCode, 77000L, () -> {}, TouchDirection.FROM_BELOW);
        PriceCondition condition3 = new PriceCondition(UUID.randomUUID(), stockCode, 78000L, () -> {}, TouchDirection.FROM_BELOW);

        PriceCondition registered1 = monitorPriceService.registerPriceCondition(condition1);
        PriceCondition registered2 = monitorPriceService.registerPriceCondition(condition2);
        PriceCondition registered3 = monitorPriceService.registerPriceCondition(condition3);

        assertThat(monitorPriceService.getConditionCount(stockCode)).isEqualTo(3);

        // 중간 조건 삭제
        boolean removed = monitorPriceService.removePriceCondition(registered2.getId());

        //then
        assertThat(removed).isTrue();
        assertThat(monitorPriceService.getConditionCount(stockCode)).isEqualTo(2);
        assertThat(monitorPriceService.getCondition(registered2.getId())).isEmpty();

        // 없는 조건 삭제 시도
        boolean notRemoved = monitorPriceService.removePriceCondition(registered2.getId());
        assertThat(notRemoved).isFalse();
    }

    @DisplayName("종목의 모든 조건을 한 번에 삭제할 수 있다.")
    @Test
    public void canRemoveAllConditionsForStock() {
        //given
        String stockCode = "005930";
        PriceCondition condition1 = new PriceCondition(UUID.randomUUID(), stockCode, 76000L, () -> {}, TouchDirection.FROM_BELOW);
        PriceCondition condition2 = new PriceCondition(UUID.randomUUID(), stockCode, 77000L, () -> {}, TouchDirection.FROM_BELOW);
        PriceCondition condition3 = new PriceCondition(UUID.randomUUID(), stockCode, 78000L, () -> {}, TouchDirection.FROM_BELOW);

        monitorPriceService.registerPriceCondition(condition1);
        monitorPriceService.registerPriceCondition(condition2);
        monitorPriceService.registerPriceCondition(condition3);

        assertThat(monitorPriceService.getConditionCount(stockCode)).isEqualTo(3);

        //when
        int removedCount = monitorPriceService.removeAllConditions(stockCode);

        //then
        assertThat(removedCount).isEqualTo(3);
        assertThat(monitorPriceService.getConditionCount(stockCode)).isEqualTo(0);
        assertThat(monitorPriceService.getTotalConditionCount()).isEqualTo(0);
    }

    @DisplayName("등록된 모든 조건을 조회할 수 있다.")
    @Test
    public void canGetAllConditions() {
        //given
        PriceCondition condition1 = new PriceCondition(UUID.randomUUID(), "005930", 76000L, () -> {}, TouchDirection.FROM_BELOW);
        PriceCondition condition2 = new PriceCondition(UUID.randomUUID() ,"000660", 86000L, () -> {}, TouchDirection.FROM_BELOW);
        PriceCondition condition3 = new PriceCondition(UUID.randomUUID(), "005930", 77000L, () -> {}, TouchDirection.FROM_BELOW);

        //when
        monitorPriceService.registerPriceCondition(condition1);
        monitorPriceService.registerPriceCondition(condition2);
        monitorPriceService.registerPriceCondition(condition3);

        List<PriceCondition> allConditions = monitorPriceService.getAllConditions();
        List<PriceCondition> samsungConditions = monitorPriceService.getConditions("005930");

        //then
        assertThat(allConditions).hasSize(3);
        assertThat(samsungConditions).hasSize(2);
        assertThat(samsungConditions).allMatch(condition -> "005930".equals(condition.getStockCode()));
    }

    @DisplayName("여러 종목에 각각 여러 조건을 등록하여 모니터링할 수 있다.")
    @Test
    public void canMonitorMultipleStocksWithMultipleConditions() {
        //given
        AtomicInteger totalAchieved = new AtomicInteger(0);

        //when
        // 삼성전자 - 2개 조건
        PriceCondition samsung1 = new PriceCondition(UUID.randomUUID(), "005930", 76000L,
                () -> {
                    System.out.println("삼성전자 76000원 달성!");
                    totalAchieved.incrementAndGet();
                }, TouchDirection.FROM_BELOW);
        PriceCondition samsung2 = new PriceCondition(UUID.randomUUID(),"005930", 77000L,
                () -> {
                    System.out.println("삼성전자 77000원 달성!");
                    totalAchieved.incrementAndGet();
                }, TouchDirection.FROM_BELOW);

        // SK하이닉스 - 2개 조건
        PriceCondition sk1 = new PriceCondition(UUID.randomUUID(), "000660", 86000L,
                () -> {
                    System.out.println("SK하이닉스 86000원 달성!");
                    totalAchieved.incrementAndGet();
                }, TouchDirection.FROM_BELOW);
        PriceCondition sk2 = new PriceCondition(UUID.randomUUID(), "000660", 87000L,
                () -> {
                    System.out.println("SK하이닉스 87000원 달성!");
                    totalAchieved.incrementAndGet();
                }, TouchDirection.FROM_BELOW);

        monitorPriceService.registerPriceCondition(samsung1);
        monitorPriceService.registerPriceCondition(samsung2);
        monitorPriceService.registerPriceCondition(sk1);
        monitorPriceService.registerPriceCondition(sk2);

        assertThat(monitorPriceService.getTotalConditionCount()).isEqualTo(4);

        monitorPriceService.startMonitoring();

        //then
        // 최소 2개 조건 달성까지 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> totalAchieved.get() >= 2);

        System.out.println("총 달성된 조건 수: " + totalAchieved.get());
        assertThat(totalAchieved.get()).isGreaterThanOrEqualTo(2);
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
