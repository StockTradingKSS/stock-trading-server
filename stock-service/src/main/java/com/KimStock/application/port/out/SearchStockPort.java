package com.KimStock.application.port.out;

import com.KimStock.domain.model.Stock;
import reactor.core.publisher.Flux;

public interface SearchStockPort {
    /**
     * 종목명에 특정 키워드가 포함된 주식을 검색합니다.
     * 
     * @param keyword 검색할 키워드
     * @return 검색된 주식 종목 Flux
     */
    Flux<Stock> findByNameContaining(String keyword);
    
    /**
     * 정확한 종목명과 일치하는 주식을 검색합니다.
     * 
     * @param exactName 정확한 종목명
     * @return 검색된 주식 종목 Flux
     */
    Flux<Stock> findByExactName(String exactName);
}
