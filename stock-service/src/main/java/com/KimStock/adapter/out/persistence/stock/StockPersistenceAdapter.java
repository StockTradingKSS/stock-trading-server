package com.KimStock.adapter.out.persistence.stock;

import com.KimStock.application.port.out.SaveStockListPort;
import com.KimStock.domain.model.Stock;
import com.common.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class StockPersistenceAdapter implements SaveStockListPort {
    private final R2dbcEntityTemplate template;

    @Override
    public void saveStockList(List<Stock> stockList) {
        log.info("Saving {} stocks to database", stockList.size());

        // 중복 제거 (같은 코드의 주식이 여러 번 포함된 경우 방지)
        Set<String> uniqueCodes = new HashSet<>();
        List<Stock> uniqueStockList = new ArrayList<>();

        for (Stock stock : stockList) {
            // 코드 길이 제한 확인 (20자 초과시 잘라내기)
            String code = stock.getCode();
            if (code.length() > 20) {
                code = code.substring(0, 20);
                log.warn("Stock code truncated: {} -> {}", stock.getCode(), code);
                // 코드가 수정된 새 Stock 객체 생성
                stock = Stock.builder()
                        .code(code)
                        .name(stock.getName())
                        .listCount(stock.getListCount())
                        .auditInfo(stock.getAuditInfo())
                        .regDay(stock.getRegDay())
                        .state(stock.getState())
                        .marketCode(stock.getMarketCode())
                        .marketName(stock.getMarketName())
                        .upName(stock.getUpName())
                        .upSizeName(stock.getUpSizeName())
                        .companyClassName(stock.getCompanyClassName())
                        .orderWarning(stock.getOrderWarning())
                        .isNxtEnable(stock.isNxtEnable())
                        .build();
            }

            // 중복 제거
            if (!uniqueCodes.contains(code)) {
                uniqueCodes.add(code);
                uniqueStockList.add(stock);
            }
        }

        // 테이블 비우기
        template.getDatabaseClient().sql("TRUNCATE TABLE stock")
                .fetch()
                .rowsUpdated()
                .doOnNext(count -> log.info("Truncated stock table"))
                .then(processStockListInChunks(uniqueStockList))
                .subscribe(
                        total -> log.info("Successfully saved {} unique stocks", total),
                        error -> log.error("Error saving stocks: {}", error.getMessage())
                );
    }

    private Mono<Integer> processStockListInChunks(List<Stock> stockList) {
        // 더 작은 청크 크기 사용 (PostgreSQL 파라미터 제한)
        int chunkSize = 10;
        List<List<Stock>> chunks = new ArrayList<>();

        for (int i = 0; i < stockList.size(); i += chunkSize) {
            chunks.add(stockList.subList(i, Math.min(i + chunkSize, stockList.size())));
        }

        return Flux.fromIterable(chunks)
                .flatMap(this::saveStockChunk)
                .reduce(0, Integer::sum);
    }

    private Mono<Integer> saveStockChunk(List<Stock> chunk) {
        if (chunk.isEmpty()) {
            return Mono.just(0);
        }

        // 개별 INSERT 문 사용 - UPSERT는 사용하지 않음 (테이블을 이미 비웠기 때문)
        return Flux.fromIterable(chunk)
                .flatMap(stock -> {
                    String sql = "INSERT INTO stock (code, name, list_count, audit_info, " +
                            "reg_day, state, market_code, market_name, up_name, up_size_name, " +
                            "company_class_name, order_warning, nxt_enable) " +
                            "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)";

                    return template.getDatabaseClient().sql(sql)
                            .bind("$1", stock.getCode())
                            .bind("$2", stock.getName())
                            .bind("$3", stock.getListCount())
                            .bind("$4", stock.getAuditInfo())
                            .bind("$5", stock.getRegDay())
                            .bind("$6", stock.getState())
                            .bind("$7", stock.getMarketCode())
                            .bind("$8", stock.getMarketName())
                            .bind("$9", stock.getUpName())
                            .bind("$10", stock.getUpSizeName())
                            .bind("$11", stock.getCompanyClassName())
                            .bind("$12", stock.getOrderWarning())
                            .bind("$13", stock.isNxtEnable())
                            .fetch()
                            .rowsUpdated()
                            .onErrorResume(e -> {
                                log.error("Error inserting stock {}: {}", stock.getCode(), e.getMessage());
                                return Mono.just(0L);
                            });
                })
                .reduce(0, (total, count) -> total + count.intValue());
    }
}
