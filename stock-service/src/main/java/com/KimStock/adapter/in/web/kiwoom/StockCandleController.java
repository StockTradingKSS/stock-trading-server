package com.KimStock.adapter.in.web.kiwoom;

import com.KimStock.application.port.out.LoadStockChartPort;
import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import com.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
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
            @PathVariable(name = "stock-code") String stockCode,
            @RequestParam(name = "candle-interval")
            @Parameter
            CandleInterval candleInterval,
            @RequestParam(name = "lastDateTime")
            @Parameter(
                    description = "마지막 조회 시간",
                    example = "2023-04-30T10:00:00",
                    schema = @Schema(type = "string", format = "date-time")
            ) LocalDateTime lastDateTime
    ) {

        return loadStockChartPort.loadStockCandleListBy(stockCode, candleInterval, lastDateTime);
    }
}


