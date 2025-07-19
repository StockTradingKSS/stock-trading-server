-- 주식 종목명 검색을 위한 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_stock_name ON stock(name);

-- 부분 일치 검색 성능 향상을 위한 추가 인덱스 (선택 사항)
-- PostgreSQL의 pg_trgm 확장을 사용하려면 먼저 확장을 활성화
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- CREATE INDEX IF NOT EXISTS idx_stock_name_gin ON stock USING gin(name gin_trgm_ops);
