package com.KimStock.adapter.in.web.kiwoom;

import com.KimStock.application.port.out.LoadAccountBalancePort;
import com.KimStock.application.port.out.LoadMarketListPort;
import com.KimStock.application.port.out.LoadStockListPort;
import com.KimStock.domain.model.AccountBalance;
import com.KimStock.domain.model.type.MarketType;
import com.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class KiwoomApiController {

    private final LoadStockListPort loadStockListPort;
    private final LoadMarketListPort loadMarketListPort;
    private final LoadAccountBalancePort loadAccountBalancePort;

    @GetMapping("/stocks")
    @Operation(summary = "종목 정보 리스트 조회", description = "시장 구분에 따른 종목 정보 리스트를 조회합니다.")
    public Mono<List<StockResponse>> getStockInfoList(
            @Parameter(description = "KOSPI, KOSDAQ", example = "KOSDAQ")
            @RequestParam(defaultValue = "KOSDAQ") MarketType marketType) {

        return loadStockListPort.loadStockInfoListBy(marketType, null, null)
                .map(marketList -> marketList.stream()
                        .map(StockResponse::of)
                        .toList());
    }

    @GetMapping("/markets/codes")
    @Operation(summary = "업종코드 리스트 조회", description = "시장 구분에 따른 업종코드 리스트를 조회합니다.")
    public Mono<List<MarketResponse>> getMarketCodeList(
            @Parameter(description = "KOSPI, KOSDAQ", example = "KOSDAQ")
            @RequestParam(defaultValue = "KOSDAQ") MarketType marketType) {

        return loadMarketListPort.loadMarketListBy(marketType, null, null)
                .map(marketList -> marketList.stream()
                        .map(MarketResponse::of)
                        .toList());
    }

    @GetMapping("/account/balance")
    @Operation(summary = "업종코드 리스트 조회", description = "시장 구분에 따른 업종코드 리스트를 조회합니다.")
    public Mono<AccountBalanceResponse> getMarketCodeList() {

        return loadAccountBalancePort.loadAccountBalance()
                .map(AccountBalanceResponse::of);
    }
}
