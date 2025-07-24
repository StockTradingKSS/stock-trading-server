package com.kokimstocktrading.adapter.out.persistence.stock;

import com.kokimstocktrading.application.port.out.SearchStockPort;
import com.kokimstocktrading.domain.model.Stock;
import com.common.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class StockSearchPersistenceAdapter implements SearchStockPort {

    private final StockRepository stockRepository;

    @Override
    public List<Stock> findByNameContaining(String keyword) {
        return stockRepository.findByNameContaining(keyword)
                .stream()
                .map(StockEntity::toDomain)
                .toList();
    }

    @Override
    public List<Stock> findByExactName(String exactName) {
        return stockRepository.findByNameIgnoreCase(exactName)
                .stream()
                .map(StockEntity::toDomain)
                .toList();
    }
}
