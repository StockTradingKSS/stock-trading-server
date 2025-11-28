package com.kokimstocktrading.adapter.out.persistence.stock;

import com.common.PersistenceAdapter;
import com.kokimstocktrading.application.stock.port.out.SearchStockPort;
import com.kokimstocktrading.domain.stock.Stock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
