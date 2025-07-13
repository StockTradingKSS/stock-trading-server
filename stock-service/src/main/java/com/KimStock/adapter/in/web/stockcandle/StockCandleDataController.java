package com.KimStock.adapter.in.web.stockcandle;

import com.KimStock.application.port.out.LoadStockChartPort;
import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stock-candle")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "주식 캔들 데이터", description = "주식 캔들 데이터 관리 API")
public class StockCandleDataController {

    private final LoadStockChartPort loadStockChartPort;

    @GetMapping()
    @Operation(summary = "삼성전자 일봉 데이터 저장", description = "삼성전자의 가능한 모든 일봉 데이터를 조회하여 저장합니다.")
    public Mono<List<StockCandle>> getStockCandles(
            @RequestParam String stockCode,
            @RequestParam CandleInterval candleInterval,
            @RequestParam(required = false) LocalDateTime lastDateTime
            ) {
        log.info("삼성전자 일봉 데이터 저장 요청");

        return loadStockChartPort.loadStockCandleListBy(stockCode, candleInterval, lastDateTime);
    }
}
