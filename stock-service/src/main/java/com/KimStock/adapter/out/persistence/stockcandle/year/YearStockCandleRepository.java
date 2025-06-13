package com.KimStock.adapter.out.persistence.stockcandle.year;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface YearStockCandleRepository extends ReactiveCrudRepository<YearStockCandleEntity, Long> {
    
    Flux<YearStockCandleEntity> findByCodeOrderByOpenTimeDesc(String code);
    
    Flux<YearStockCandleEntity> findByCode(String code);
}
