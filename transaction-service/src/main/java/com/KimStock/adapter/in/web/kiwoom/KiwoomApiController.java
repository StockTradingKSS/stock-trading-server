package com.KimStock.adapter.in.web.kiwoom;

import com.KimStock.adapter.out.kiwoom.service.KiwoomApiAdapter;
import com.KimStock.domain.model.type.MarketType;
import com.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@WebAdapter
@RestController
@RequestMapping("/api/kiwoom")
@Tag(name = "Kiwoom API", description = "키움증권 API 연동 서비스")
public class KiwoomApiController {

    private final KiwoomApiAdapter kiwoomApiAdapter;

    public KiwoomApiController(KiwoomApiAdapter kiwoomApiAdapter) {
        this.kiwoomApiAdapter = kiwoomApiAdapter;
    }

    @GetMapping("/stocks")
    @Operation(summary = "종목 정보 리스트 조회", description = "시장 구분에 따른 종목 정보 리스트를 조회합니다.")
    public Mono<List<StockResponse>> getStockInfoList(
            @Parameter(description = "KOSPI, KOSDAQ", example = "KOSDAQ")
            @RequestParam(defaultValue = "KOSDAQ") MarketType marketType) {

        return kiwoomApiAdapter.getStockListByMarketCode(marketType)
                .map(marketList -> marketList.stream()
                        .map(StockResponse::of)
                        .toList());
    }

    @GetMapping("/markets/codes")
    @Operation(summary = "업종코드 리스트 조회", description = "시장 구분에 따른 업종코드 리스트를 조회합니다.")
    public Mono<List<MarketResponse>> getMarketCodeList(
            @Parameter(description = "KOSPI, KOSDAQ", example = "KOSDAQ")
            @RequestParam(defaultValue = "KOSDAQ") MarketType marketType) {

        return kiwoomApiAdapter.getMarketListByMarketCode(marketType)
                .map(marketList -> marketList.stream()
                        .map(MarketResponse::of)
                        .toList());
    }
}
