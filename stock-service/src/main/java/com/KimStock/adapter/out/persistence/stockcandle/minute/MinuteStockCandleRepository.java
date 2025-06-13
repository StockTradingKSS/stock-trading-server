package com.KimStock.adapter.out.persistence.stockcandle.minute;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MinuteStockCandleRepository extends ReactiveCrudRepository<MinuteStockCandleEntity, Long> {
    
    Flux<MinuteStockCandleEntity> findByCodeOrderByOpenTimeDesc(String code);
    
    Flux<MinuteStockCandleEntity> findByCode(String code);
}
