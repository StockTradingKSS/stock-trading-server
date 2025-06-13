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

        // Day Stock Candle 테이블 생성
        Mono<Void> createDayStockCandleTable = template.getDatabaseClient().sql(
                "CREATE TABLE IF NOT EXISTS day_stock_candle (" +
                "id BIGSERIAL PRIMARY KEY," +
                "code VARCHAR(20) NOT NULL," +
                "current_price BIGINT," +
                "previous_price BIGINT," +
                "volume BIGINT," +
                "open_price BIGINT," +
                "high_price BIGINT," +
                "low_price BIGINT," +
                "close_price BIGINT," +
                "open_time TIMESTAMP)"
        ).then();

        // Minute Stock Candle 테이블 생성
        Mono<Void> createMinuteStockCandleTable = template.getDatabaseClient().sql(
                "CREATE TABLE IF NOT EXISTS minute_stock_candle (" +
                "id BIGSERIAL PRIMARY KEY," +
                "code VARCHAR(20) NOT NULL," +
                "current_price BIGINT," +
                "previous_price BIGINT," +
                "volume BIGINT," +
                "open_price BIGINT," +
                "high_price BIGINT," +
                "low_price BIGINT," +
                "close_price BIGINT," +
                "open_time TIMESTAMP)"
        ).then();

        // Week Stock Candle 테이블 생성
        Mono<Void> createWeekStockCandleTable = template.getDatabaseClient().sql(
                "CREATE TABLE IF NOT EXISTS week_stock_candle (" +
                "id BIGSERIAL PRIMARY KEY," +
                "code VARCHAR(20) NOT NULL," +
                "current_price BIGINT," +
                "previous_price BIGINT," +
                "volume BIGINT," +
                "open_price BIGINT," +
                "high_price BIGINT," +
                "low_price BIGINT," +
                "close_price BIGINT," +
                "open_time TIMESTAMP)"
        ).then();

        // Month Stock Candle 테이블 생성
        Mono<Void> createMonthStockCandleTable = template.getDatabaseClient().sql(
                "CREATE TABLE IF NOT EXISTS month_stock_candle (" +
                "id BIGSERIAL PRIMARY KEY," +
                "code VARCHAR(20) NOT NULL," +
                "current_price BIGINT," +
                "previous_price BIGINT," +
                "volume BIGINT," +
                "open_price BIGINT," +
                "high_price BIGINT," +
                "low_price BIGINT," +
                "close_price BIGINT," +
                "open_time TIMESTAMP)"
        ).then();

        // Year Stock Candle 테이블 생성
        Mono<Void> createYearStockCandleTable = template.getDatabaseClient().sql(
                "CREATE TABLE IF NOT EXISTS year_stock_candle (" +
                "id BIGSERIAL PRIMARY KEY," +
                "code VARCHAR(20) NOT NULL," +
                "current_price BIGINT," +
                "previous_price BIGINT," +
                "volume BIGINT," +
                "open_price BIGINT," +
                "high_price BIGINT," +
                "low_price BIGINT," +
                "close_price BIGINT," +
                "open_time TIMESTAMP)"
        ).then();

        // 인덱스 생성
        Mono<Void> createIndexes = template.getDatabaseClient().sql(
                "CREATE INDEX IF NOT EXISTS idx_stock_market_code ON stock(market_code);" +
                "CREATE INDEX IF NOT EXISTS idx_day_stock_candle_code_time ON day_stock_candle(code, open_time DESC);" +
                "CREATE INDEX IF NOT EXISTS idx_minute_stock_candle_code_time ON minute_stock_candle(code, open_time DESC);" +
                "CREATE INDEX IF NOT EXISTS idx_week_stock_candle_code_time ON week_stock_candle(code, open_time DESC);" +
                "CREATE INDEX IF NOT EXISTS idx_month_stock_candle_code_time ON month_stock_candle(code, open_time DESC);" +
                "CREATE INDEX IF NOT EXISTS idx_year_stock_candle_code_time ON year_stock_candle(code, open_time DESC)"
        ).then();

        // 순차적으로 실행
        createStockTable
                .then(createDayStockCandleTable)
                .then(createMinuteStockCandleTable)
                .then(createWeekStockCandleTable)
                .then(createMonthStockCandleTable)
                .then(createYearStockCandleTable)
                .then(createIndexes)
                .doOnSuccess(v -> log.info("PostgreSQL tables initialized successfully"))
                .doOnError(e -> log.error("Error initializing PostgreSQL tables", e))
                .subscribe();
    }
}
