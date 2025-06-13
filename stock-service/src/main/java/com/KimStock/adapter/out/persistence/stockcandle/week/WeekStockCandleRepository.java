package com.KimStock.adapter.out.persistence.stockcandle.week;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface WeekStockCandleRepository extends ReactiveCrudRepository<WeekStockCandleEntity, Long> {
    
    Flux<WeekStockCandleEntity> findByCodeOrderByOpenTimeDesc(String code);
    
    Flux<WeekStockCandleEntity> findByCode(String code);
}
