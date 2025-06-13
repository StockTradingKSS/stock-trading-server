package com.KimStock.adapter.out.persistence.stockcandle.month;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MonthStockCandleRepository extends ReactiveCrudRepository<MonthStockCandleEntity, Long> {
    
    Flux<MonthStockCandleEntity> findByCodeOrderByOpenTimeDesc(String code);
    
    Flux<MonthStockCandleEntity> findByCode(String code);
}
