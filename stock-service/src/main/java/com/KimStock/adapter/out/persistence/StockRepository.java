package com.KimStock.adapter.out.persistence;

import com.KimStock.adapter.out.persistence.entity.StockJpaEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface StockRepository extends ReactiveCrudRepository<StockJpaEntity, String> {
    
    Flux<StockJpaEntity> findByNameContainingIgnoreCase(String keyword);

    Flux<StockJpaEntity> findByNameEqualsIgnoreCase(String exactName);
}
