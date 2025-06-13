package com.KimStock.adapter.out.persistence.stock;

import com.KimStock.application.port.out.SearchStockPort;
import com.KimStock.domain.model.Stock;
import com.common.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class StockSearchPersistenceAdapter implements SearchStockPort {

    private final StockRepository stockRepository;
    
    @Override
    public Flux<Stock> findByNameContaining(String keyword) {
        return stockRepository.findByNameContainingIgnoreCase(keyword)
                        .map(StockEntity::toDomain);
    }
    
    @Override
    public Flux<Stock> findByExactName(String exactName) {
        return stockRepository.findByNameEqualsIgnoreCase(exactName)
                .map(StockEntity::toDomain);
    }
}
