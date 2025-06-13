package com.KimStock.adapter.out.persistence.stock;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface StockRepository extends ReactiveCrudRepository<StockEntity, String> {
    
    Flux<StockEntity> findByNameContainingIgnoreCase(String keyword);

    Flux<StockEntity> findByNameEqualsIgnoreCase(String exactName);
}
