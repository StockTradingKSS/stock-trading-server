package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.application.realtime.out.SubscribeRealTimeQuotePort;
import com.kokimstocktrading.domain.realtime.RealTimeQuote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitorPriceService {
    private final SubscribeRealTimeQuotePort subscribeRealTimeQuotePort;
    
    // 가격 조건 저장 (종목코드 -> 조건 정보)
    private final Map<String, PriceCondition> priceConditions = new ConcurrentHashMap<>();
    
    // 모니터링 구독 관리
    private final Map<String, Disposable> monitoringSubscriptions = new ConcurrentHashMap<>();

    /**
     * 가격 조건 등록
     * @param stockCode 종목코드
     * @param targetPrice 목표가격
     * @param callback 조건 만족 시 실행할 콜백
     */
    public void registerPriceCondition(String stockCode, Long targetPrice, Runnable callback) {
        PriceCondition condition = new PriceCondition(targetPrice, callback);
        priceConditions.put(stockCode, condition);
        log.info("가격 조건 등록: 종목={}, 목표가격={}", stockCode, targetPrice);
    }

    /**
     * 모니터링 시작
     */
    public void startMonitoring() {
        if (priceConditions.isEmpty()) {
            log.warn("등록된 가격 조건이 없습니다.");
            return;
        }

        List<String> stockCodes = List.copyOf(priceConditions.keySet());
        log.info("가격 모니터링 시작: 대상 종목={}", stockCodes);

        Flux<RealTimeQuote> realTimeQuoteFlux = subscribeRealTimeQuotePort.subscribeStockQuote(stockCodes);
        
        Disposable subscription = realTimeQuoteFlux
                .doOnNext(this::checkPriceCondition)
                .doOnError(error -> log.error("가격 모니터링 중 오류 발생", error))
                .subscribe();

        // 전체 모니터링 구독 저장
        monitoringSubscriptions.put("ALL", subscription);
    }

    /**
     * 개별 종목 모니터링 시작
     */
    public void startMonitoring(String stockCode) {
        if (!priceConditions.containsKey(stockCode)) {
            log.warn("종목 {}에 대한 가격 조건이 등록되지 않았습니다.", stockCode);
            return;
        }

        log.info("개별 가격 모니터링 시작: 종목={}", stockCode);

        Flux<RealTimeQuote> realTimeQuoteFlux = subscribeRealTimeQuotePort.subscribeStockQuote(List.of(stockCode));
        
        Disposable subscription = realTimeQuoteFlux
                .filter(quote -> stockCode.equals(quote.item()))
                .doOnNext(this::checkPriceCondition)
                .doOnError(error -> log.error("종목 {} 가격 모니터링 중 오류 발생", stockCode, error))
                .subscribe();

        monitoringSubscriptions.put(stockCode, subscription);
    }

    /**
     * 가격 조건 체크
     */
    private void checkPriceCondition(RealTimeQuote quote) {
        String stockCode = quote.item();
        PriceCondition condition = priceConditions.get(stockCode);
        
        if (condition == null) {
            return;
        }

        try {
            double currentPrice = Double.parseDouble(quote.currentPrice());
            
            if (currentPrice >= condition.targetPrice()) {
                log.info("가격 조건 달성! 종목={}, 현재가={}, 목표가={}", 
                    stockCode, currentPrice, condition.targetPrice());
                
                // 콜백 실행
                condition.callback().run();
                
                // 조건 달성 후 해당 종목 모니터링 중지
                stopMonitoring(stockCode);
            }
        } catch (NumberFormatException e) {
            log.warn("가격 파싱 실패: 종목={}, 가격={}", stockCode, quote.currentPrice());
        }
    }

    /**
     * 특정 종목 모니터링 중지
     */
    public void stopMonitoring(String stockCode) {
        Disposable subscription = monitoringSubscriptions.remove(stockCode);
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("종목 {} 모니터링 중지", stockCode);
        }
        
        // 조건도 제거
        priceConditions.remove(stockCode);
    }

    /**
     * 전체 모니터링 중지
     */
    public void stopAllMonitoring() {
        monitoringSubscriptions.values().forEach(subscription -> {
            if (!subscription.isDisposed()) {
                subscription.dispose();
            }
        });
        
        monitoringSubscriptions.clear();
        priceConditions.clear();
        log.info("전체 가격 모니터링 중지");
    }

    /**
     * 가격 조건 정보
     */
    private record PriceCondition(Long targetPrice, Runnable callback) {}
}
