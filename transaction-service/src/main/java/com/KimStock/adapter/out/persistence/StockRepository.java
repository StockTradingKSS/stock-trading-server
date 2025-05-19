package com.KimStock.adapter.out.persistence;

import com.KimStock.adapter.out.persistence.entity.StockJpaEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface StockRepository extends ReactiveCrudRepository<StockJpaEntity, String> {
    
    // 시장 코드별 주식 조회
    Flux<StockJpaEntity> findByMarketCode(String marketCode);
    
    // 시장 이름으로 주식 조회
    Flux<StockJpaEntity> findByMarketName(String marketName);
    
    // 사용자 정의 쿼리 (필요한 경우)
    @Query("SELECT * FROM stock WHERE up_name = :upName")
    Flux<StockJpaEntity> findByIndustry(String upName);
}
