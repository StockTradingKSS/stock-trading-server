package com.KimStock.application.port.in;

import com.KimStock.domain.model.Stock;

import java.util.List;

public interface SearchStockUseCase {
    /**
     * 종목명에 포함된 키워드로 주식 종목을 검색합니다.
     *
     * @param keyword 검색할 종목명 키워드
     * @return 검색된 주식 종목 리스트
     */
    List<Stock> searchStocksByName(String keyword);

    /**
     * 정확한 종목명으로 주식 종목을 검색합니다.
     *
     * @param exactName 정확한 종목명
     * @return 검색된 주식 종목 리스트
     */
    List<Stock> findStocksByExactName(String exactName);
}
