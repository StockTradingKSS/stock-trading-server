package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.application.realtime.out.SubscribeRealTimeQuotePort;
import com.kokimstocktrading.domain.realtime.RealTimeQuote;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
public class DetectPriceTest {

    /**
     *   종목 코드 : "005930", 시작 가격 : "75000", 종목 이름 : 삼성전자
     *   종목 코드 : "000660", 시작 가격 : "85000", 종목 이름 : SK하이닉스
     *   종목 코드 : "035420", 시작 가격 : "850000", 종목 이름 : NAVER
     *   종목 코드 : "051910", 시작 가격 : "4500" , 종목 이름 : LG화학
     *   종목 코드 : "006400", 시작 가격 : "35000", 종목 이름 : 삼성SDI
     *   다음 시세의 변동폭은 전 시세의 +- 5 % 입니다.
     */
    private SubscribeRealTimeQuotePort mockAdapter;

    @BeforeEach
    void setUp() {
        mockAdapter = new MockRealTimeQuoteAdapter();
    }

    @DisplayName("구독한 종목들의 시세를 수신할 수 있다.")
    @Test
    public void canSubscribeMultipleStocks() {
        //given
        List<String> stockCodes = List.of("005930", "000660", "035420");
        AtomicInteger receivedCount = new AtomicInteger(0);
        
        //when
        Flux<RealTimeQuote> realTimeQuoteFlux = mockAdapter.subscribeStockQuote(stockCodes);
        
        //then
        StepVerifier.create(realTimeQuoteFlux.take(6).timeout(Duration.ofSeconds(10)))
                .thenConsumeWhile(quote -> {
                    assertThat(stockCodes).contains(quote.item());
                    receivedCount.incrementAndGet();
                    return true;
                })
                .verifyComplete();
        
        assertThat(receivedCount.get()).isEqualTo(6);
    }

    @DisplayName("특정 가격에 도달하면 조건 감시가 동작한다.")
    @Test
    public void detectSpecificPriceCondition() {
        //given
        String stockCode = "005930";
        String targetPrice = "76000"; // 삼성전자 76000원
        
        //when
        Flux<RealTimeQuote> realTimeQuoteFlux = mockAdapter.subscribeStockQuote(List.of(stockCode));
        
        //then
        StepVerifier.create(
                realTimeQuoteFlux
                    .filter(quote -> {
                        double currentPrice = Double.parseDouble(quote.currentPrice());
                        double target = Double.parseDouble(targetPrice);
                        return currentPrice >= target;
                    })
                    .doOnNext(quote -> System.out.println("조건감시"))
                    .take(1)
                    .timeout(Duration.ofSeconds(10))
        )
        .expectNextCount(1)
        .verifyComplete();
    }
}
