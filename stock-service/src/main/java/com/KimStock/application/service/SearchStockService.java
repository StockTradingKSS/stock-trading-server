package com.KimStock.application.service;

import com.KimStock.application.port.in.SearchStockUseCase;
import com.KimStock.application.port.out.SearchStockPort;
import com.KimStock.domain.model.Stock;
import com.common.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SearchStockService implements SearchStockUseCase {
    
    private final SearchStockPort searchStockPort;
    
    @Override
    public Flux<Stock> searchStocksByName(String keyword) {
        log.info("Searching stocks by keyword: {}", keyword);
        
        // 검색 키워드가 비어있는 경우 빈 결과 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return Flux.empty();
        }
        
        // 키워드를 소문자로 변환하고 공백 제거
        String sanitizedKeyword = keyword.trim();
        
        return searchStockPort.findByNameContaining(sanitizedKeyword)
                .doOnComplete(() -> log.info("Stock search completed"))
                .doOnError(e -> log.error("Error searching stocks by keyword: {}", e.getMessage()));
    }
    
    @Override
    public Flux<Stock> findStocksByExactName(String exactName) {
        log.info("Finding stocks by exact name: {}", exactName);
        
        if (exactName == null || exactName.trim().isEmpty()) {
            return Flux.empty();
        }
        
        return searchStockPort.findByExactName(exactName.trim())
                .doOnComplete(() -> log.info("Exact name stock search completed"))
                .doOnError(e -> log.error("Error finding stocks by exact name: {}", e.getMessage()));
    }
}
