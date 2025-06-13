package com.KimStock.adapter.out.persistence.stockcandle.day;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DayStockCandleRepository extends ReactiveCrudRepository<DayStockCandleEntity, Long> {
    
    Flux<DayStockCandleEntity> findByCodeOrderByOpenTimeDesc(String code);
    
    Flux<DayStockCandleEntity> findByCode(String code);
}
