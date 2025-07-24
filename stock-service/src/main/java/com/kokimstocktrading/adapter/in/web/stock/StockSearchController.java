package com.kokimstocktrading.adapter.in.web.stock;

import com.kokimstocktrading.application.port.in.SearchStockUseCase;
import com.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@WebAdapter
@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stock API", description = "Stock Search 관련 API")
@RequiredArgsConstructor
@Slf4j
public class StockSearchController {
    private final SearchStockUseCase searchStockUseCase;

    @GetMapping("/search")
    @Operation(summary = "종목명으로 주식 검색", description = "종목명에 포함된 키워드로 주식을 검색합니다")
    public List<StockSearchResponse> searchStocks(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword) {

        log.info("Searching stocks with keyword: {}", keyword);

        return searchStockUseCase.searchStocksByName(keyword)
                .stream()
                .map(StockSearchResponse::fromDomain)
                .toList();
    }

    @GetMapping("/exact")
    @Operation(summary = "정확한 종목명으로 주식 검색", description = "정확한 종목명과 일치하는 주식을 검색합니다")
    public List<StockSearchResponse> findStocksByExactName(
            @Parameter(description = "정확한 종목명", required = true)
            @RequestParam String name) {

        log.info("Finding stocks with exact name: {}", name);

        return searchStockUseCase.findStocksByExactName(name)
                .stream()
                .map(StockSearchResponse::fromDomain)
                .toList();
    }
}
