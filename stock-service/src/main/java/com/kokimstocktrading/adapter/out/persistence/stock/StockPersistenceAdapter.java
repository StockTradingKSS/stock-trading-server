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

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class StockPersistenceAdapter implements SaveStockListPort {

  private final StockRepository stockRepository;

  @Override
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
      saveUsingBatchUpsert(uniqueStockList);
      log.info("Successfully saved {} unique stocks using UPSERT", uniqueStockList.size());
    } catch (Exception e) {
      log.error("Error saving stocks: {}", e.getMessage(), e);
      throw e;
    }
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

    // 청크 크기 설정 (PostgreSQL 트랜잭션 행 제한을 피하기 위해)
    int chunkSize = 500;
    int totalChunks = (int) Math.ceil((double) stockEntities.size() / chunkSize);

    log.info("Processing {} stocks in {} chunks of size {}", stockEntities.size(), totalChunks,
        chunkSize);

    // 청크로 나눠서 처리
    for (int i = 0; i < stockEntities.size(); i += chunkSize) {
      int end = Math.min(i + chunkSize, stockEntities.size());
      List<StockEntity> chunk = stockEntities.subList(i, end);

      int chunkNumber = (i / chunkSize) + 1;
      log.debug("Processing chunk {}/{} ({} stocks)", chunkNumber, totalChunks, chunk.size());

      // 배열로 변환
      String[] codes = chunk.stream().map(StockEntity::getCode).toArray(String[]::new);
      String[] names = chunk.stream().map(StockEntity::getName).toArray(String[]::new);
      Long[] listCounts = chunk.stream().map(StockEntity::getListCount).toArray(Long[]::new);
      String[] auditInfos = chunk.stream().map(StockEntity::getAuditInfo)
          .toArray(String[]::new);
      String[] regDays = chunk.stream().map(StockEntity::getRegDay).toArray(String[]::new);
      String[] states = chunk.stream().map(StockEntity::getState).toArray(String[]::new);
      String[] marketCodes = chunk.stream().map(StockEntity::getMarketCode)
          .toArray(String[]::new);
      String[] marketNames = chunk.stream().map(StockEntity::getMarketName)
          .toArray(String[]::new);
      String[] upNames = chunk.stream().map(StockEntity::getUpName).toArray(String[]::new);
      String[] upSizeNames = chunk.stream().map(StockEntity::getUpSizeName)
          .toArray(String[]::new);
      String[] companyClassNames = chunk.stream().map(StockEntity::getCompanyClassName)
          .toArray(String[]::new);
      String[] orderWarnings = chunk.stream().map(StockEntity::getOrderWarning)
          .toArray(String[]::new);
      Boolean[] nxtEnables = chunk.stream().map(StockEntity::isNxtEnable)
          .toArray(Boolean[]::new);

      // 배치 UPSERT 실행
      stockRepository.batchUpsertStocks(
          codes, names, listCounts, auditInfos, regDays, states,
          marketCodes, marketNames, upNames, upSizeNames,
          companyClassNames, orderWarnings, nxtEnables
      );

      log.debug("Completed chunk {}/{}", chunkNumber, totalChunks);
    }

    long endTime = System.currentTimeMillis();
    log.info("Batch UPSERT completed in {}ms for {} stocks", (endTime - startTime),
        stockList.size());
  }
}
