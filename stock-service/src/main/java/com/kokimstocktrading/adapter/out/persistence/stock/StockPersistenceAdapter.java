package com.kokimstocktrading.adapter.out.persistence.stock;

import com.common.PersistenceAdapter;
import com.kokimstocktrading.application.stock.port.out.SaveStockListPort;
import com.kokimstocktrading.domain.stock.Stock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class StockPersistenceAdapter implements SaveStockListPort {

  private final StockRepository stockRepository;

  @Override
  @Transactional
  public void saveStockList(List<Stock> stockList) {
    log.info("Saving {} stocks to database using UPSERT", stockList.size());

    // 중복 제거 (같은 코드의 주식이 여러 번 포함된 경우 방지)
    Set<String> uniqueCodes = new HashSet<>();
    List<Stock> uniqueStockList = new ArrayList<>();

    stockList.forEach(stock -> {
      String code = stock.getCode();
      if (!uniqueCodes.contains(code)) {
        uniqueCodes.add(code);
        uniqueStockList.add(stock);
      }
    });

    try {
      // 방법 1: 개별 UPSERT (작은 데이터셋용)
      if (uniqueStockList.size() <= 1000) {
        saveUsingIndividualUpsert(uniqueStockList);
      } else {
        // 방법 2: 배치 UPSERT (큰 데이터셋용)
        saveUsingBatchUpsert(uniqueStockList);
      }

      log.info("Successfully saved {} unique stocks using UPSERT", uniqueStockList.size());
    } catch (Exception e) {
      log.error("Error saving stocks: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * 개별 UPSERT 방식 (작은 데이터셋용)
   */
  private void saveUsingIndividualUpsert(List<Stock> stockList) {
    log.info("Starting individual UPSERT for {} stocks", stockList.size());
    long startTime = System.currentTimeMillis();

    List<StockEntity> stockEntities = stockList.stream()
        .map(StockEntity::from)
        .toList();

    int batchSize = 100;
    int totalBatches = (stockEntities.size() + batchSize - 1) / batchSize;
    log.info("Processing {} stocks in {} batches of size {}", stockEntities.size(), totalBatches,
        batchSize);

    for (int i = 0; i < stockEntities.size(); i += batchSize) {
      int endIndex = Math.min(i + batchSize, stockEntities.size());
      List<StockEntity> batch = stockEntities.subList(i, endIndex);

      long batchStartTime = System.currentTimeMillis();
      log.debug("Executing UPSERT batch {}/{} with {} stocks", (i / batchSize + 1), totalBatches,
          batch.size());

      // 배치로 UPSERT 실행
      batch.forEach(stock -> {
        log.trace("Upserting stock: code={}, name={}", stock.getCode(), stock.getName());
        stockRepository.upsertStock(stock);
      });

      long batchEndTime = System.currentTimeMillis();
      log.debug("Batch {}/{} completed in {}ms", (i / batchSize + 1), totalBatches,
          (batchEndTime - batchStartTime));
    }

    long endTime = System.currentTimeMillis();
    log.info("Individual UPSERT completed in {}ms for {} stocks", (endTime - startTime),
        stockList.size());
  }

  /**
   * 배치 UPSERT 방식 (큰 데이터셋용)
   */
  private void saveUsingBatchUpsert(List<Stock> stockList) {
    log.info("Starting batch UPSERT for {} stocks", stockList.size());
    long startTime = System.currentTimeMillis();

    List<StockEntity> stockEntities = stockList.stream()
        .map(StockEntity::from)
        .toList();

    log.debug("Converting {} stock entities to arrays", stockEntities.size());

    // 배열로 변환
    String[] codes = stockEntities.stream().map(StockEntity::getCode).toArray(String[]::new);
    String[] names = stockEntities.stream().map(StockEntity::getName).toArray(String[]::new);
    Long[] listCounts = stockEntities.stream().map(StockEntity::getListCount).toArray(Long[]::new);
    String[] auditInfos = stockEntities.stream().map(StockEntity::getAuditInfo)
        .toArray(String[]::new);
    String[] regDays = stockEntities.stream().map(StockEntity::getRegDay).toArray(String[]::new);
    String[] states = stockEntities.stream().map(StockEntity::getState).toArray(String[]::new);
    String[] marketCodes = stockEntities.stream().map(StockEntity::getMarketCode)
        .toArray(String[]::new);
    String[] marketNames = stockEntities.stream().map(StockEntity::getMarketName)
        .toArray(String[]::new);
    String[] upNames = stockEntities.stream().map(StockEntity::getUpName).toArray(String[]::new);
    String[] upSizeNames = stockEntities.stream().map(StockEntity::getUpSizeName)
        .toArray(String[]::new);
    String[] companyClassNames = stockEntities.stream().map(StockEntity::getCompanyClassName)
        .toArray(String[]::new);
    String[] orderWarnings = stockEntities.stream().map(StockEntity::getOrderWarning)
        .toArray(String[]::new);
    Boolean[] nxtEnables = stockEntities.stream().map(StockEntity::isNxtEnable)
        .toArray(Boolean[]::new);

    log.debug("Executing batch UPSERT with {} stocks", stockEntities.size());

    // 배치 UPSERT 실행
    stockRepository.batchUpsertStocks(
        codes, names, listCounts, auditInfos, regDays, states,
        marketCodes, marketNames, upNames, upSizeNames,
        companyClassNames, orderWarnings, nxtEnables
    );

    long endTime = System.currentTimeMillis();
    log.info("Batch UPSERT completed in {}ms for {} stocks", (endTime - startTime),
        stockList.size());
  }
}
