package com.KimStock.application.service;

import com.KimStock.adapter.out.external.chart.KiwoomDayChartClient;
import com.KimStock.adapter.out.persistence.stock.StockEntity;
import com.KimStock.adapter.out.persistence.stock.StockRepository;
import com.KimStock.adapter.out.persistence.stockcandle.StockCandlePersistenceAdapter;
import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockCandleService {

    private final KiwoomDayChartClient kiwoomDayChartClient;
    private final StockCandlePersistenceAdapter stockCandlePersistenceAdapter;
    private final StockRepository stockRepository;

    /**
     * 삼성전자 일봉 데이터를 최대한 많이 조회해서 병렬 저장
     */
    public Mono<Void> saveSamsungDayCandles() {
        String samsungCode = "005930";
        LocalDateTime today = LocalDateTime.of(2025, 6, 14, 0, 0);
        
        log.info("삼성전자({}) 일봉 데이터 병렬 저장 시작", samsungCode);
        
        // 1. 먼저 삼성전자 Stock 데이터 확인 및 삽입
        return ensureSamsungStockExists(samsungCode)
                .then(loadAllDayCandles(samsungCode, today))
                .flatMap(candles -> {
                    if (candles.isEmpty()) {
                        log.warn("조회된 일봉 데이터가 없습니다.");
                        return Mono.empty();
                    }
                    
                    log.info("총 {}개의 일봉 데이터를 병렬로 저장합니다.", candles.size());
                    long startTime = System.currentTimeMillis();
                    
                    // 대용량 데이터를 청크로 나누어 병렬 저장
                    return Flux.fromIterable(candles)
                            .buffer(1000) // 1000개씩 청크로 나눔
                            .flatMap(chunk -> {
                                log.info("{}개 청크 저장 시작...", chunk.size());
                                return stockCandlePersistenceAdapter.bulkSaveStockCandles(chunk, CandleInterval.DAY)
                                        .subscribeOn(Schedulers.boundedElastic()) // 병렬 처리
                                        .doOnSuccess(v -> log.info("{}개 청크 저장 완료", chunk.size()));
                            }, 5) // 최대 5개 청크 동시 처리
                            .then()
                            .doOnSuccess(v -> {
                                long endTime = System.currentTimeMillis();
                                log.info("병렬 저장 완료: {}개 데이터, 총 소요시간: {}ms", 
                                        candles.size(), endTime - startTime);
                            });
                })
                .doOnSuccess(v -> log.info("삼성전자 일봉 데이터 병렬 저장 완료"))
                .doOnError(e -> log.error("삼성전자 일봉 데이터 저장 실패: {}", e.getMessage()));
    }

    /**
     * 삼성전자 Stock 데이터가 존재하는지 확인하고, 없으면 생성
     */
    private Mono<Void> ensureSamsungStockExists(String stockCode) {
        return stockRepository.findById(stockCode)
                .switchIfEmpty(
                    // 삼성전자 데이터가 없으면 생성
                    Mono.defer(() -> {
                        log.info("삼성전자 Stock 데이터가 없어서 생성합니다.");
                        StockEntity samsung = StockEntity.builder()
                                .code(stockCode)
                                .name("삼성전자")
                                .marketCode("KOSPI")
                                .marketName("코스피")
                                .state("정상")
                                .nxtEnable(true)
                                .build();
                        samsung.setAsExisting(); // 새 엔티티가 아님을 표시
                        
                        return stockRepository.save(samsung);
                    })
                )
                .then()
                .doOnSuccess(v -> log.info("삼성전자 Stock 데이터 확인/생성 완료"));
    }

    /**
     * 가능한 모든 일봉 데이터를 순차적으로 조회
     * 키움 API는 한 번에 최대 600개까지 조회 가능하므로 반복 호출
     */
    private Mono<List<StockCandle>> loadAllDayCandles(String stockCode, LocalDateTime startDate) {
        return loadDayCandlesRecursively(stockCode, startDate, List.of(), 0);
    }

    private Mono<List<StockCandle>> loadDayCandlesRecursively(String stockCode, LocalDateTime currentDate, 
                                                            List<StockCandle> allCandles, int requestCount) {

        KiwoomDayChartClient.DayStockCandleRequest request = 
            KiwoomDayChartClient.DayStockCandleRequest.of(stockCode, false, currentDate);
        
        log.info("일봉 데이터 조회 #{}: 종목={}, 기준일={}", requestCount + 1, stockCode, currentDate);
        
        return kiwoomDayChartClient.loadDayCandles(request)
                .flatMap(newCandles -> {
                    if (newCandles.isEmpty()) {
                        log.info("더 이상 조회할 데이터가 없습니다. 총 {}개 조회됨", allCandles.size());
                        return Mono.just(allCandles);
                    }
                    
                    // 새로 조회된 데이터를 기존 리스트에 추가
                    List<StockCandle> updatedCandles = new java.util.ArrayList<>(allCandles);
                    updatedCandles.addAll(newCandles);
                    
                    log.info("{}개 데이터 조회됨, 현재까지 총 {}개", newCandles.size(), updatedCandles.size());
                    
                    // 다음 배치를 위해 가장 오래된 날짜로 기준일 업데이트
                    LocalDateTime nextDate = newCandles.getLast().getOpenTime();
                    
                    log.info("다음 배치 조회 준비: 다음 기준일={}", nextDate);
                    
                    // API 제한 준수를 위한 딜레이
                    return Mono.delay(java.time.Duration.ofMillis(500))
                            .then(loadDayCandlesRecursively(stockCode, nextDate,
                                                          updatedCandles, requestCount + 1));
                })
                .onErrorResume(e -> {
                    log.error("일봉 데이터 조회 중 오류 발생: {}", e.getMessage());
                    if (allCandles.isEmpty()) {
                        return Mono.error(e);
                    } else {
                        log.info("부분적으로 조회된 {}개 데이터를 반환합니다.", allCandles.size());
                        return Mono.just(allCandles);
                    }
                });
    }
}
