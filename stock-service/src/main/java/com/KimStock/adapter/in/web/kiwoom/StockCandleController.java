package com.KimStock.adapter.in.web.kiwoom;

import com.KimStock.application.port.out.LoadStockChartPort;
import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import com.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@WebAdapter
@RequestMapping("/api/stock-candle")
@Tag(name = "Stock Candle API", description = "주식 캔들 API")
@Slf4j
@RequiredArgsConstructor
@RestController
public class StockCandleController {

    private final LoadStockChartPort loadStockChartPort;

    @GetMapping(value = "/{stock-code}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "주식 캔들 조회", description = "주식 캔들 조회")
    public Mono<List<StockCandle>> getMockStockRealTimeQuote(
            @PathVariable(name = "stock-code") String stockCode
    ) {

        return loadStockChartPort.loadStockCandleListBy(stockCode, CandleInterval.MINUTE);
    }
}


