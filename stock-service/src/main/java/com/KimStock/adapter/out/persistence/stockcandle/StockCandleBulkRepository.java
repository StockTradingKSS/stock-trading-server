package com.KimStock.adapter.out.persistence.stockcandle;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface StockCandleBulkRepository<T> {
    
    Mono<Void> bulkInsert(List<T> entities);
    
    default Flux<T> bulkSaveAll(Flux<T> entities) {
        return entities.buffer(1000)
                      .flatMap(batch -> bulkInsert(batch).thenMany(Flux.fromIterable(batch)));
    }
}
