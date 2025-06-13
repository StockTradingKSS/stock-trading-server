-- Stock 테이블
CREATE TABLE IF NOT EXISTS stock (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    list_count BIGINT,
    audit_info VARCHAR(255),
    reg_day VARCHAR(20),
    state VARCHAR(20),
    market_code VARCHAR(20),
    market_name VARCHAR(50),
    up_name VARCHAR(50),
    up_size_name VARCHAR(50),
    company_class_name VARCHAR(50),
    order_warning VARCHAR(50),
    nxt_enable BOOLEAN DEFAULT FALSE
);

-- Day Stock Candle 테이블
CREATE TABLE IF NOT EXISTS day_stock_candle (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    current_price BIGINT,
    previous_price BIGINT,
    volume BIGINT,
    open_price BIGINT,
    high_price BIGINT,
    low_price BIGINT,
    close_price BIGINT,
    open_time TIMESTAMP
);

-- Minute Stock Candle 테이블
CREATE TABLE IF NOT EXISTS minute_stock_candle (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    current_price BIGINT,
    previous_price BIGINT,
    volume BIGINT,
    open_price BIGINT,
    high_price BIGINT,
    low_price BIGINT,
    close_price BIGINT,
    open_time TIMESTAMP
);

-- Week Stock Candle 테이블
CREATE TABLE IF NOT EXISTS week_stock_candle (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    current_price BIGINT,
    previous_price BIGINT,
    volume BIGINT,
    open_price BIGINT,
    high_price BIGINT,
    low_price BIGINT,
    close_price BIGINT,
    open_time TIMESTAMP
);

-- Month Stock Candle 테이블
CREATE TABLE IF NOT EXISTS month_stock_candle (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    current_price BIGINT,
    previous_price BIGINT,
    volume BIGINT,
    open_price BIGINT,
    high_price BIGINT,
    low_price BIGINT,
    close_price BIGINT,
    open_time TIMESTAMP
);

-- Year Stock Candle 테이블
CREATE TABLE IF NOT EXISTS year_stock_candle (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    current_price BIGINT,
    previous_price BIGINT,
    volume BIGINT,
    open_price BIGINT,
    high_price BIGINT,
    low_price BIGINT,
    close_price BIGINT,
    open_time TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_stock_market_code ON stock(market_code);
CREATE INDEX IF NOT EXISTS idx_stock_name ON stock(name);

-- 캔들 테이블 인덱스 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_day_stock_candle_code_time ON day_stock_candle(code, open_time DESC);
CREATE INDEX IF NOT EXISTS idx_minute_stock_candle_code_time ON minute_stock_candle(code, open_time DESC);
CREATE INDEX IF NOT EXISTS idx_week_stock_candle_code_time ON week_stock_candle(code, open_time DESC);
CREATE INDEX IF NOT EXISTS idx_month_stock_candle_code_time ON month_stock_candle(code, open_time DESC);
CREATE INDEX IF NOT EXISTS idx_year_stock_candle_code_time ON year_stock_candle(code, open_time DESC);

-- 캔들 테이블 고유 제약조건 (중복 방지)
CREATE UNIQUE INDEX IF NOT EXISTS uk_day_stock_candle_code_time ON day_stock_candle(code, open_time);
CREATE UNIQUE INDEX IF NOT EXISTS uk_minute_stock_candle_code_time ON minute_stock_candle(code, open_time);
CREATE UNIQUE INDEX IF NOT EXISTS uk_week_stock_candle_code_time ON week_stock_candle(code, open_time);
CREATE UNIQUE INDEX IF NOT EXISTS uk_month_stock_candle_code_time ON month_stock_candle(code, open_time);
CREATE UNIQUE INDEX IF NOT EXISTS uk_year_stock_candle_code_time ON year_stock_candle(code, open_time);
