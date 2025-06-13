package com.KimStock.adapter.out.persistence.stockcandle.year;

import com.KimStock.adapter.out.persistence.stockcandle.StockCandleBulkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class YearStockCandleBulkRepositoryImpl implements StockCandleBulkRepository<YearStockCandleEntity> {

    private final DatabaseClient databaseClient;
    
    // 배치 크기와 동시성 설정
    private static final int BATCH_SIZE = 100;
    private static final int CONCURRENCY = 10; // 동시에 처리할 배치 수

    @Override
    public Mono<Void> bulkInsert(List<YearStockCandleEntity> entities) {
        if (entities.isEmpty()) {
            return Mono.empty();
        }

        log.info("YearCandle 병렬 Bulk insert 시작: {}개 엔티티, 배치크기: {}, 동시성: {}", 
                entities.size(), BATCH_SIZE, CONCURRENCY);
        
        long startTime = System.currentTimeMillis();
        
        // 병렬로 배치 처리
        return Flux.fromIterable(entities)
                .buffer(BATCH_SIZE)
                .flatMap(this::insertBatch, CONCURRENCY) // 병렬 처리
                .then()
                .doOnSuccess(v -> {
                    long endTime = System.currentTimeMillis();
                    log.info("YearCandle 병렬 Bulk insert 완료: {}개 엔티티, 총 소요시간: {}ms", 
                            entities.size(), endTime - startTime);
                });
    }

    private Mono<Void> insertBatch(List<YearStockCandleEntity> batch) {
        if (batch.isEmpty()) {
            return Mono.empty();
        }

        long startTime = System.currentTimeMillis();
        
        StringBuilder sql = new StringBuilder("""
            INSERT INTO year_stock_candle 
            (code, current_price, previous_price, volume, open_price, high_price, low_price, close_price, open_time) 
            VALUES 
            """);

        // VALUES 절 구성 - 명명된 파라미터 사용
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(:code").append(i)
               .append(", :currentPrice").append(i)
               .append(", :previousPrice").append(i)
               .append(", :volume").append(i)
               .append(", :openPrice").append(i)
               .append(", :highPrice").append(i)
               .append(", :lowPrice").append(i)
               .append(", :closePrice").append(i)
               .append(", :openTime").append(i).append(")");
        }

        // 중복 데이터 무시
        sql.append(" ON CONFLICT (code, open_time) DO NOTHING");

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());

        // 명명된 파라미터 바인딩
        for (int i = 0; i < batch.size(); i++) {
            YearStockCandleEntity entity = batch.get(i);
            
            spec = spec.bind("code" + i, entity.getCode())
                      .bind("currentPrice" + i, entity.getCurrentPrice() != null ? entity.getCurrentPrice() : 0L)
                      .bind("previousPrice" + i, entity.getPreviousPrice() != null ? entity.getPreviousPrice() : 0L)
                      .bind("volume" + i, entity.getVolume() != null ? entity.getVolume() : 0L)
                      .bind("openPrice" + i, entity.getOpenPrice() != null ? entity.getOpenPrice() : 0L)
                      .bind("highPrice" + i, entity.getHighPrice() != null ? entity.getHighPrice() : 0L)
                      .bind("lowPrice" + i, entity.getLowPrice() != null ? entity.getLowPrice() : 0L)
                      .bind("closePrice" + i, entity.getClosePrice() != null ? entity.getClosePrice() : 0L)
                      .bind("openTime" + i, entity.getOpenTime() != null ? entity.getOpenTime() : LocalDateTime.now());
        }

        return spec.then()
                .subscribeOn(Schedulers.boundedElastic()) // 별도 스레드에서 실행
                .doOnSuccess(v -> {
                    long endTime = System.currentTimeMillis();
                    log.debug("YearCandle 배치 삽입 완료: {}개, 소요시간: {}ms, 스레드: {}", 
                            batch.size(), endTime - startTime, Thread.currentThread().getName());
                })
                .onErrorResume(e -> {
                    log.error("YearCandle Bulk insert 실패 (배치크기: {}), 개별 저장으로 폴백: {}", batch.size(), e.getMessage());
                    return fallbackToParallelIndividualInsert(batch);
                });
    }

    /**
     * Bulk insert 실패 시 병렬 개별 저장으로 폴백
     */
    private Mono<Void> fallbackToParallelIndividualInsert(List<YearStockCandleEntity> batch) {
        return Flux.fromIterable(batch)
                .flatMap(this::insertSingle, 20) // 개별 저장도 병렬 처리
                .then()
                .doOnSuccess(v -> log.info("YearCandle 병렬 개별 저장 완료: {}개", batch.size()));
    }

    private Mono<Void> insertSingle(YearStockCandleEntity entity) {
        String sql = """
            INSERT INTO year_stock_candle 
            (code, current_price, previous_price, volume, open_price, high_price, low_price, close_price, open_time) 
            VALUES (:code, :currentPrice, :previousPrice, :volume, :openPrice, :highPrice, :lowPrice, :closePrice, :openTime)
            ON CONFLICT (code, open_time) DO NOTHING
            """;

        return databaseClient.sql(sql)
                .bind("code", entity.getCode())
                .bind("currentPrice", entity.getCurrentPrice() != null ? entity.getCurrentPrice() : 0L)
                .bind("previousPrice", entity.getPreviousPrice() != null ? entity.getPreviousPrice() : 0L)
                .bind("volume", entity.getVolume() != null ? entity.getVolume() : 0L)
                .bind("openPrice", entity.getOpenPrice() != null ? entity.getOpenPrice() : 0L)
                .bind("highPrice", entity.getHighPrice() != null ? entity.getHighPrice() : 0L)
                .bind("lowPrice", entity.getLowPrice() != null ? entity.getLowPrice() : 0L)
                .bind("closePrice", entity.getClosePrice() != null ? entity.getClosePrice() : 0L)
                .bind("openTime", entity.getOpenTime() != null ? entity.getOpenTime() : LocalDateTime.now())
                .then()
                .subscribeOn(Schedulers.boundedElastic()); // 별도 스레드에서 실행
    }

    @Override
    public Flux<YearStockCandleEntity> bulkSaveAll(Flux<YearStockCandleEntity> entities) {
        return entities.buffer(BATCH_SIZE)
                      .flatMap(batch -> bulkInsert(batch).thenMany(Flux.fromIterable(batch)), CONCURRENCY);
    }
}
