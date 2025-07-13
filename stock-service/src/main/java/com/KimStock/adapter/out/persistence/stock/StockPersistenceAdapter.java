package com.KimStock.adapter.out.persistence.stock;

import com.KimStock.application.port.out.SaveStockListPort;
import com.KimStock.domain.model.Stock;
import com.common.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class StockPersistenceAdapter implements SaveStockListPort {
    private final StockRepository stockRepository;

    @Override
    @Transactional
    public void saveStockList(List<Stock> stockList) {
        log.info("Saving {} stocks to database", stockList.size());

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
            // 기존 데이터 삭제
            stockRepository.deleteAll();
            log.info("Deleted all existing stocks");

            // Stock → StockEntity 변환 후 저장
            List<StockEntity> stockEntities = uniqueStockList.stream()
                    .map(StockEntity::from)
                    .toList();

            // 새 데이터 저장 (배치로 처리)
            int batchSize = 1000;
            for (int i = 0; i < stockEntities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, stockEntities.size());
                List<StockEntity> batch = stockEntities.subList(i, endIndex);
                stockRepository.saveAll(batch);
                log.debug("Saved batch {}-{}", i + 1, endIndex);
            }

            log.info("Successfully saved {} unique stocks", uniqueStockList.size());
        } catch (Exception e) {
            log.error("Error saving stocks: {}", e.getMessage(), e);
            throw e;
        }
    }
}
