package com.kokimstocktrading.adapter.in.web.kiwoom;

import com.common.WebAdapter;
import com.kokimstocktrading.application.candle.port.out.LoadStockCandlePort;
import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.candle.StockCandle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@WebAdapter
@RequestMapping("/api/stock-candle")
@Tag(name = "Stock Candle API", description = "주식 캔들 API")
@Slf4j
@RequiredArgsConstructor
@RestController
public class StockCandleController {

  private final LoadStockCandlePort loadStockCandlePort;

  @GetMapping("/{stock-code}")
  @Operation(summary = "주식 캔들 조회", description = "주식 캔들 조회")
  public Mono<List<StockCandle>> getStockCandle(
      @PathVariable(name = "stock-code") String stockCode,
      @RequestParam(name = "candle-interval")
      @Parameter
      CandleInterval candleInterval,
      @RequestParam(name = "fromDateTime")
      @Parameter(
          description = "시작 조회 시간",
          example = "2023-04-30T10:00:00",
          schema = @Schema(type = "string", format = "date-time")
      ) LocalDateTime fromDateTime,
      @RequestParam(name = "toDateTime")
      @Parameter(
          description = "끝 조회 시간(미포함)",
          example = "2023-04-30T10:00:00",
          schema = @Schema(type = "string", format = "date-time")
      ) LocalDateTime toDateTime
  ) {
    return loadStockCandlePort.loadStockCandleListBy(stockCode, candleInterval, fromDateTime,
        toDateTime);
  }
}
