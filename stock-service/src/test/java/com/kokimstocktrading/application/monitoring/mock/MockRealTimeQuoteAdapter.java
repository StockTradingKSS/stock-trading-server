package com.kokimstocktrading.application.monitoring.mock;

import com.kokimstocktrading.application.realtime.out.SubscribeRealTimeQuotePort;
import com.kokimstocktrading.domain.realtime.RealTimeQuote;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MockRealTimeQuoteAdapter implements SubscribeRealTimeQuotePort {

    private final Map<String, StockPriceInfo> stockPriceMap = new ConcurrentHashMap<>();
    private final Map<List<String>, Sinks.Many<RealTimeQuote>> subscriptionMap = new ConcurrentHashMap<>();
    private final Map<String, PriceScenario> priceScenarios = new ConcurrentHashMap<>();

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

    // 가격 시나리오 정보
    private static class PriceScenario {
        private final List<BigDecimal> priceSequence;
        private final AtomicInteger currentIndex;

        public PriceScenario(List<BigDecimal> priceSequence) {
            this.priceSequence = new ArrayList<>(priceSequence);
            this.currentIndex = new AtomicInteger(0);
        }

        public BigDecimal getNextPrice() {
            int index = currentIndex.getAndIncrement();
            if (index >= priceSequence.size()) {
                // 시나리오 끝에 도달하면 마지막 가격 반복
                return priceSequence.get(priceSequence.size() - 1);
            }
            return priceSequence.get(index);
        }

        public boolean hasMorePrices() {
            return currentIndex.get() < priceSequence.size();
        }

        public void reset() {
            currentIndex.set(0);
        }
    }

    public MockRealTimeQuoteAdapter() {
        // 시나리오 파일 로드
        loadPriceScenarios();
        // 초기 주식 가격 설정
        initializeDefaultStocks();
    }

    private void loadPriceScenarios() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("stock-price-scenarios.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 주석이나 빈 줄 건너뛰기
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(":");
                if (parts.length == 3) {
                    String stockCode = parts[0];
                    BigDecimal startPrice = new BigDecimal(parts[1]);
                    String[] priceStrings = parts[2].split(",");
                    
                    List<BigDecimal> priceSequence = new ArrayList<>();
                    priceSequence.add(startPrice); // 시작 가격 추가
                    
                    for (String priceStr : priceStrings) {
                        priceSequence.add(new BigDecimal(priceStr.trim()));
                    }
                    
                    priceScenarios.put(stockCode, new PriceScenario(priceSequence));
                    log.info("가격 시나리오 로드: 종목={}, 시퀀스={}", stockCode, priceSequence);
                }
            }
        } catch (IOException e) {
            log.warn("가격 시나리오 파일 로드 실패, 기본값 사용", e);
        }
    }

    private void initializeDefaultStocks() {
        // 시나리오가 있는 종목은 시나리오의 시작 가격 사용
        for (Map.Entry<String, PriceScenario> entry : priceScenarios.entrySet()) {
            String stockCode = entry.getKey();
            BigDecimal startPrice = entry.getValue().priceSequence.get(0);
            stockPriceMap.put(stockCode, new StockPriceInfo(startPrice));
        }

        // 시나리오가 없는 기본 종목들
        stockPriceMap.putIfAbsent("005930", new StockPriceInfo(new BigDecimal("75000")));
        stockPriceMap.putIfAbsent("000660", new StockPriceInfo(new BigDecimal("85000")));
        stockPriceMap.putIfAbsent("035420", new StockPriceInfo(new BigDecimal("850000")));
        stockPriceMap.putIfAbsent("051910", new StockPriceInfo(new BigDecimal("4500")));
        stockPriceMap.putIfAbsent("006400", new StockPriceInfo(new BigDecimal("35000")));
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
        // 1초마다 시나리오 기반 시세 생성
        Flux.interval(java.time.Duration.ofSeconds(1))
                .doOnNext(tick -> {
                    for (String stockCode : stockCodes) {
                        RealTimeQuote quote = generateScenarioBasedQuote(stockCode);
                        if (quote != null) {
                            sink.tryEmitNext(quote);
                            log.debug("시나리오 시세 전송: 종목={}, 가격={}", stockCode, quote.currentPrice());
                        }
                    }
                })
                .subscribe();
    }

    private RealTimeQuote generateScenarioBasedQuote(String stockCode) {
        StockPriceInfo priceInfo = stockPriceMap.get(stockCode);
        if (priceInfo == null) {
            return null;
        }

        BigDecimal newPrice;
        PriceScenario scenario = priceScenarios.get(stockCode);
        
        if (scenario != null) {
            // 시나리오가 있으면 시나리오 따라감
            newPrice = scenario.getNextPrice();
        } else {
            // 시나리오가 없으면 기존 랜덤 로직
            return generateRandomQuote(stockCode);
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
                .tradeTime(LocalDateTime.now())
                .build();
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
                .tradeTime(LocalDateTime.now())
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
                        RealTimeQuote quote = generateScenarioBasedQuote(stockCode);
                        if (quote != null) {
                            entry.getValue().tryEmitNext(quote);
                        }
                    });
        }
    }

    public BigDecimal getCurrentPrice(String stockCode) {
        StockPriceInfo priceInfo = stockPriceMap.get(stockCode);
        return priceInfo != null ? priceInfo.currentPrice : null;
    }

    // 시나리오 리셋 (테스트용)
    public void resetScenarios() {
        priceScenarios.values().forEach(PriceScenario::reset);
        log.info("모든 가격 시나리오 리셋 완료");
    }

    // 특정 종목 시나리오 리셋
    public void resetScenario(String stockCode) {
        PriceScenario scenario = priceScenarios.get(stockCode);
        if (scenario != null) {
            scenario.reset();
            log.info("종목 {} 시나리오 리셋 완료", stockCode);
        }
    }
}
