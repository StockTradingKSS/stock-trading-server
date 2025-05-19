package com.KimStock.adapter.out.persistence.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class PostgresInitializer implements CommandLineRunner {

    private final R2dbcEntityTemplate template;

    @Override
    public void run(String... args) {
        log.info("Initializing PostgreSQL tables");

        // Stock 테이블 생성
        Mono<Void> createStockTable = template.getDatabaseClient().sql(
                "CREATE TABLE IF NOT EXISTS stock (" +
                "code VARCHAR(20) PRIMARY KEY," +
                "name VARCHAR(100) NOT NULL," +
                "list_count BIGINT," +
                "audit_info VARCHAR(255)," +
                "reg_day VARCHAR(20)," +
                "state VARCHAR(20)," +
                "market_code VARCHAR(20)," +
                "market_name VARCHAR(50)," +
                "up_name VARCHAR(50)," +
                "up_size_name VARCHAR(50)," +
                "company_class_name VARCHAR(50)," +
                "order_warning VARCHAR(50)," +
                "nxt_enable BOOLEAN DEFAULT FALSE)"
        ).then();

        // 인덱스 생성 (필요에 따라)
        Mono<Void> createIndexes = template.getDatabaseClient().sql(
                "CREATE INDEX IF NOT EXISTS idx_stock_market_code ON stock(market_code)"
        ).then();

        // 순차적으로 실행
        createStockTable
                .then(createIndexes)
                .doOnSuccess(v -> log.info("PostgreSQL tables initialized successfully"))
                .doOnError(e -> log.error("Error initializing PostgreSQL tables", e))
                .subscribe();
    }
}
