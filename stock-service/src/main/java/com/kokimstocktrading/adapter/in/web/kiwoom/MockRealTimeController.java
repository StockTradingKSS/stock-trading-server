package com.kokimstocktrading.adapter.in.web.kiwoom;

import com.kokimstocktrading.domain.realtime.RealTimeQuote;
import com.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@WebAdapter
@RestController
@RequestMapping("/api/mock")
@Tag(name = "Mock Data API", description = "테스트용 목 데이터 API")
@Slf4j
public class MockRealTimeController {

    private static final Map<String, StockMockData> STOCK_MOCKS = new ConcurrentHashMap<>();
    private static final String[] STOCK_CODES = {"005930", "035720", "000660", "373220", "207940"};
    private static final String[] STOCK_NAMES = {"삼성전자", "카카오", "SK하이닉스", "LG에너지솔루션", "삼성바이오로직스"};
    private static final int[] BASE_PRICES = {80000, 50000, 130000, 410000, 750000};

    static {
        // 초기 목 데이터 생성
        for (int i = 0; i < STOCK_CODES.length; i++) {
            STOCK_MOCKS.put(STOCK_CODES[i], new StockMockData(
                    STOCK_CODES[i],
                    STOCK_NAMES[i],
                    BASE_PRICES[i]
            ));
        }
    }

    // 모든 클라이언트에게 데이터를 브로드캐스트하기 위한 싱크
    private final Sinks.Many<RealTimeQuote> mockDataSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Flux<RealTimeQuote> mockDataFlux = mockDataSink.asFlux().cache(100);

    // 목 데이터 생성 상태
    private final AtomicInteger activeSubscribers = new AtomicInteger(0);
    private boolean isGeneratingData = false;
    private final Random random = new Random();

    @GetMapping(value = "/realtime/quote", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Mock 실시간 주식 시세 조회", description = "테스트용 목 실시간 주식 시세를 Server-Sent Events로 제공합니다.")
    public Flux<ServerSentEvent<RealTimeQuoteResponse>> getMockStockRealTimeQuote() {
        // 첫 구독자가 연결되면 데이터 생성 시작
        if (activeSubscribers.incrementAndGet() == 1) {
            startGeneratingMockData();
        }

        log.info("Mock 실시간 시세 연결됨. 현재 구독자 수: {}", activeSubscribers.get());

        return mockDataFlux
                .map(RealTimeQuoteResponse::from)
                .map(data -> ServerSentEvent.<RealTimeQuoteResponse>builder()
                        .id(data.stockCode())
                        .event("quote")
                        .data(data)
                        .build())
                .doFinally(signalType -> {
                    // 모든 구독자가 연결 해제되면 데이터 생성 중지
                    if (activeSubscribers.decrementAndGet() == 0) {
                        stopGeneratingMockData();
                    }
                    log.info("Mock 실시간 시세 연결 종료. 남은 구독자 수: {}", activeSubscribers.get());
                })
                // SSE 연결 유지를 위한 하트비트 추가
                .mergeWith(Flux.interval(Duration.ofSeconds(30))
                        .map(i -> ServerSentEvent.<RealTimeQuoteResponse>builder()
                                .event("heartbeat")
                                .build()));
    }

    private synchronized void startGeneratingMockData() {
        if (isGeneratingData) {
            return;
        }

        isGeneratingData = true;
        log.info("목 데이터 생성 시작");

        // 3초마다 모든 종목의 실시간 데이터 생성
        Flux.interval(Duration.ofSeconds(3))
                .takeWhile(i -> isGeneratingData)
                .flatMap(i -> Flux.fromArray(STOCK_CODES)
                        .map(this::generateMockQuote))
                .subscribe(
                        quote -> {
                            mockDataSink.tryEmitNext(quote);
                            log.debug("목 데이터 생성: {}", quote);
                        },
                        error -> log.error("목 데이터 생성 중 오류", error),
                        () -> log.info("목 데이터 생성 종료")
                );
    }

    private synchronized void stopGeneratingMockData() {
        isGeneratingData = false;
        log.info("목 데이터 생성 중지");
    }

    private RealTimeQuote generateMockQuote(String stockCode) {
        StockMockData mockData = STOCK_MOCKS.get(stockCode);

        // 가격 변동 (±2% 내외)
        int priceChange = (int) (mockData.basePrice * (random.nextDouble() * 0.04 - 0.02));
        int currentPrice = mockData.currentPrice + priceChange;

        // 현재가가 기준가의 ±20% 범위를 벗어나지 않도록 조정
        int minPrice = (int) (mockData.basePrice * 0.8);
        int maxPrice = (int) (mockData.basePrice * 1.2);
        currentPrice = Math.max(minPrice, Math.min(maxPrice, currentPrice));

        // 데이터 업데이트
        mockData.currentPrice = currentPrice;
        mockData.totalVolume += random.nextInt(1000) + 100;
        mockData.updateCount++;

        // 가격 변동에 따른 여러 값 계산
        int dailyChange = currentPrice - mockData.basePrice;
        double changeRate = (double) dailyChange / mockData.basePrice * 100;
        int askPrice = currentPrice + 100;
        int bidPrice = currentPrice - 100;
        int tradingVolume = random.nextInt(500) + 10;

        // 현재 시간
        String tradeTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        return RealTimeQuote.builder()
                .type("0B")
                .name(mockData.stockName)
                .item(stockCode)
                .currentPrice(String.valueOf(currentPrice))
                .priceChange(String.valueOf(dailyChange))
                .changeRate(String.format("%.2f", changeRate))
                .askPrice(String.valueOf(askPrice))
                .bidPrice(String.valueOf(bidPrice))
                .tradingVolume("+" + tradingVolume)
                .accumulatedVolume(String.valueOf(mockData.totalVolume))
                .accumulatedAmount(String.valueOf(mockData.totalVolume * currentPrice / 1000))
                .openPrice(String.valueOf(mockData.basePrice))
                .highPrice(String.valueOf(Math.max(currentPrice, mockData.basePrice)))
                .lowPrice(String.valueOf(Math.min(currentPrice, mockData.basePrice)))
                .tradeTime(parseTradeTime(tradeTime))
                .build();
    }

    /**
     * 목 데이터 상태 관리 클래스
     */
    private static class StockMockData {
        private final String stockCode;
        private final String stockName;
        private final int basePrice;
        private int currentPrice;
        private int totalVolume;
        private int updateCount;

        public StockMockData(String stockCode, String stockName, int basePrice) {
            this.stockCode = stockCode;
            this.stockName = stockName;
            this.basePrice = basePrice;
            this.currentPrice = basePrice;
            this.totalVolume = 0;
            this.updateCount = 0;
        }
    }

    /**
     * 체결시간 문자열(HHMMSS)을 LocalDateTime으로 변환
     * @param tradeTimeStr 체결시간 (예: "161402")
     * @return LocalDateTime (오늘 날짜 + 시분초)
     */
    private LocalDateTime parseTradeTime(String tradeTimeStr) {
        if (tradeTimeStr == null || tradeTimeStr.length() != 6) {
            return LocalDateTime.now(); // 기본값
        }

        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
            LocalTime time = LocalTime.parse(tradeTimeStr, timeFormatter);
            return LocalDateTime.of(LocalDate.now(), time);
        } catch (Exception e) {
            log.warn("체결시간 파싱 실패: {}", tradeTimeStr, e);
            return LocalDateTime.now();
        }
    }
}
