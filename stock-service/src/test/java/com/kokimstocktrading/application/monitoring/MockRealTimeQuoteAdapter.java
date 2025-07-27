package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.application.realtime.out.SubscribeRealTimeQuotePort;
import com.kokimstocktrading.domain.realtime.RealTimeQuote;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class MockRealTimeQuoteAdapter implements SubscribeRealTimeQuotePort {
    
    private final Map<String, StockPriceInfo> stockPriceMap = new ConcurrentHashMap<>();
    private final Map<List<String>, Sinks.Many<RealTimeQuote>> subscriptionMap = new ConcurrentHashMap<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");

    // 주식 가격 정보를 관리하는 내부 클래스
    private static class StockPriceInfo {
        private BigDecimal currentPrice;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private BigDecimal basePrice;
        private long accumulatedVolume = 0L;
        private BigDecimal accumulatedAmount = BigDecimal.ZERO;

        public StockPriceInfo(BigDecimal basePrice) {
            this.basePrice = basePrice;
            this.currentPrice = basePrice;
            this.openPrice = basePrice;
            this.highPrice = basePrice;
            this.lowPrice = basePrice;
        }
    }

    public MockRealTimeQuoteAdapter() {
        // 초기 주식 가격 설정 (일반적인 한국 주식 가격대)
        initializeDefaultStocks();
    }
    
    private void initializeDefaultStocks() {
        stockPriceMap.put("005930", new StockPriceInfo(new BigDecimal("75000"))); // 삼성전자
        stockPriceMap.put("000660", new StockPriceInfo(new BigDecimal("85000"))); // SK하이닉스
        stockPriceMap.put("035420", new StockPriceInfo(new BigDecimal("850000"))); // NAVER
        stockPriceMap.put("051910", new StockPriceInfo(new BigDecimal("4500")));  // LG화학
        stockPriceMap.put("006400", new StockPriceInfo(new BigDecimal("35000"))); // 삼성SDI
    }
    
    @Override
    public Flux<RealTimeQuote> subscribeStockQuote(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Flux.empty();
        }
        
        // 각 종목에 대한 기본 정보가 없으면 랜덤 생성
        for (String stockCode : stockCodes) {
            stockPriceMap.computeIfAbsent(stockCode, this::createRandomStockInfo);
        }
        
        Sinks.Many<RealTimeQuote> sink = Sinks.many().multicast().onBackpressureBuffer();
        subscriptionMap.put(stockCodes, sink);
        
        // 실시간 데이터 생성 시작
        startRealTimeDataGeneration(stockCodes, sink);
        
        log.info("Mock 실시간 시세 구독 시작: {}", stockCodes);
        return sink.asFlux();
    }
    
    private StockPriceInfo createRandomStockInfo(String stockCode) {
        // 1,000원 ~ 100,000원 사이의 랜덤 기준가
        BigDecimal basePrice = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(1000, 100000))
                .setScale(0, RoundingMode.HALF_UP);
        return new StockPriceInfo(basePrice);
    }
    
    private void startRealTimeDataGeneration(List<String> stockCodes, Sinks.Many<RealTimeQuote> sink) {
        // 각 종목별로 1-3초마다 랜덤하게 시세 생성
        Flux.interval(java.time.Duration.ofMillis(500))
                .doOnNext(tick -> {
                    for (String stockCode : stockCodes) {
                        // 30% 확률로 시세 업데이트
                        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                            RealTimeQuote quote = generateRandomQuote(stockCode);
                            sink.tryEmitNext(quote);
                        }
                    }
                })
                .subscribe();
    }
    
    private RealTimeQuote generateRandomQuote(String stockCode) {
        StockPriceInfo priceInfo = stockPriceMap.get(stockCode);
        
        // 현재가 변동 (-5% ~ +5%)
        double changeRatio = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.1; // -5% ~ +5%
        BigDecimal newPrice = priceInfo.currentPrice
                .multiply(BigDecimal.valueOf(1 + changeRatio))
                .setScale(0, RoundingMode.HALF_UP);
        
        // 최소 100원 이상 유지
        if (newPrice.compareTo(BigDecimal.valueOf(100)) < 0) {
            newPrice = BigDecimal.valueOf(100);
        }
        
        // 가격 정보 업데이트
        priceInfo.currentPrice = newPrice;
        if (newPrice.compareTo(priceInfo.highPrice) > 0) {
            priceInfo.highPrice = newPrice;
        }
        if (newPrice.compareTo(priceInfo.lowPrice) < 0) {
            priceInfo.lowPrice = newPrice;
        }
        
        // 전일대비 계산
        BigDecimal priceChange = newPrice.subtract(priceInfo.basePrice);
        double changeRate = priceChange.multiply(BigDecimal.valueOf(100))
                .divide(priceInfo.basePrice, 2, RoundingMode.HALF_UP)
                .doubleValue();
        
        // 호가 생성 (현재가 기준 ±50~200원)
        BigDecimal askPrice = newPrice.add(BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(50, 201)));
        BigDecimal bidPrice = newPrice.subtract(BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(50, 201)));
        
        // 거래량 생성
        long tradingVolume = ThreadLocalRandom.current().nextLong(1000, 10000);
        priceInfo.accumulatedVolume += tradingVolume;
        BigDecimal tradeAmount = newPrice.multiply(BigDecimal.valueOf(tradingVolume));
        priceInfo.accumulatedAmount = priceInfo.accumulatedAmount.add(tradeAmount);
        
        return RealTimeQuote.builder()
                .type("0B")
                .name(getStockName(stockCode))
                .item(stockCode)
                .currentPrice(newPrice.toString())
                .priceChange(priceChange.toString())
                .changeRate(String.format("%.2f", changeRate))
                .askPrice(askPrice.toString())
                .bidPrice(bidPrice.toString())
                .tradingVolume(String.valueOf(tradingVolume))
                .accumulatedVolume(String.valueOf(priceInfo.accumulatedVolume))
                .accumulatedAmount(priceInfo.accumulatedAmount.toString())
                .openPrice(priceInfo.openPrice.toString())
                .highPrice(priceInfo.highPrice.toString())
                .lowPrice(priceInfo.lowPrice.toString())
                .tradeTime(LocalDateTime.now().format(timeFormatter))
                .build();
    }
    
    private String getStockName(String stockCode) {
        Map<String, String> stockNames = Map.of(
                "005930", "삼성전자",
                "000660", "SK하이닉스", 
                "035420", "NAVER",
                "051910", "LG화학",
                "006400", "삼성SDI"
        );
        return stockNames.getOrDefault(stockCode, "테스트주식_" + stockCode);
    }
    
    @Override
    public boolean unsubscribeStockQuote(List<String> stockCodes) {
        Sinks.Many<RealTimeQuote> sink = subscriptionMap.remove(stockCodes);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("Mock 실시간 시세 구독 해지: {}", stockCodes);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean unsubscribeAllStockQuotes() {
        subscriptionMap.values().forEach(sink -> sink.tryEmitComplete());
        subscriptionMap.clear();
        log.info("Mock 모든 실시간 시세 구독 해지");
        return true;
    }
    
    // 테스트용 메서드들
    public void setStockPrice(String stockCode, BigDecimal price) {
        StockPriceInfo priceInfo = stockPriceMap.computeIfAbsent(stockCode, k -> new StockPriceInfo(price));
        priceInfo.currentPrice = price;
        priceInfo.basePrice = price;
    }
    
    public void simulatePriceChange(String stockCode, BigDecimal newPrice) {
        StockPriceInfo priceInfo = stockPriceMap.get(stockCode);
        if (priceInfo != null) {
            priceInfo.currentPrice = newPrice;
            
            // 구독 중인 모든 그룹에 즉시 시세 전송
            subscriptionMap.entrySet().stream()
                    .filter(entry -> entry.getKey().contains(stockCode))
                    .forEach(entry -> {
                        RealTimeQuote quote = generateRandomQuote(stockCode);
                        entry.getValue().tryEmitNext(quote);
                    });
        }
    }
    
    public BigDecimal getCurrentPrice(String stockCode) {
        StockPriceInfo priceInfo = stockPriceMap.get(stockCode);
        return priceInfo != null ? priceInfo.currentPrice : null;
    }
}
