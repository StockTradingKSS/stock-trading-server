package com.kokimstocktrading.adapter.in.web.monitoring;

import com.common.WebAdapter;
import com.kokimstocktrading.application.condition.port.in.RegisterMovingAverageCommand;
import com.kokimstocktrading.application.condition.port.in.RegisterTradingConditionUseCase;
import com.kokimstocktrading.application.condition.port.in.RegisterTrendLineCommand;
import com.kokimstocktrading.application.monitoring.MonitorPriceService;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.PriceCondition;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@WebAdapter
@RestController
@RequestMapping("/api/monitor")
@Tag(name = "Monitor API", description = "Monitor 관련 API")
@RequiredArgsConstructor
@Slf4j
public class MonitorStockController {

  private final RegisterTradingConditionUseCase registerTradingConditionUseCase;
  private final MonitorPriceService monitorPriceService;

  @PostMapping("/moving-average")
  @Operation(summary = "이평선 조건 등록", description = "이평선 조건을 등록합니다. DB에 저장하고 거래 시간이면 모니터링을 시작합니다.")
  @ResponseStatus(code = HttpStatus.OK)
  public UUID registerMovingAverageCondition(@RequestBody RegisterMovingAverageCommand command) {
    log.info("이평선 조건 등록 API 요청: {}", command);
    MovingAverageCondition condition = registerTradingConditionUseCase.registerMovingAverageCondition(
        command);
    log.info("이평선 조건 등록 성공: {}", condition.getId());
    return condition.getId();
  }

  @PostMapping("/trend-line")
  @Operation(summary = "추세선 조건 등록", description = "추세선 조건을 등록합니다. DB에 저장하고 거래 시간이면 모니터링을 시작합니다.")
  @ResponseStatus(code = HttpStatus.OK)
  public UUID registerTrendLineCondition(@RequestBody RegisterTrendLineCommand command) {
    log.info("추세선 조건 등록 API 요청: {}", command);
    TrendLineCondition condition = registerTradingConditionUseCase.registerTrendLineCondition(
        command);
    log.info("추세선 조건 등록 성공: {}", condition.getId());
    return condition.getId();
  }

  @DeleteMapping("/{conditionId}")
  @Operation(summary = "조건 삭제", description = "등록된 조건을 삭제합니다. DB에서 비활성화하고 모니터링도 중지합니다.")
  @ResponseStatus(code = HttpStatus.OK)
  public void deleteCondition(@PathVariable UUID conditionId) {
    log.info("조건 삭제 API 요청: {}", conditionId);
    registerTradingConditionUseCase.deleteCondition(conditionId);
    log.info("조건 삭제 성공: {}", conditionId);
  }

  @GetMapping("/conditions")
  @Operation(summary = "전체 모니터링 상태 조회", description = "현재 등록된 모든 가격 조건과 모니터링 상태를 조회합니다.")
  @ResponseStatus(code = HttpStatus.OK)
  public MonitoringStatusResponse getMonitoringStatus() {
    log.info("전체 모니터링 상태 조회 API 요청");

    List<PriceCondition> allConditions = monitorPriceService.getAllConditions();
    List<PriceConditionResponse> conditionResponses = allConditions.stream()
        .map(PriceConditionResponse::from)
        .toList();

    MonitoringStatusResponse response = new MonitoringStatusResponse(
        monitorPriceService.getTotalConditionCount(),
        monitorPriceService.getMonitoringStocks().size(),
        monitorPriceService.getMonitoringStocks(),
        conditionResponses
    );

    log.info("전체 모니터링 상태 조회 성공: 총 {}개 조건, {}개 종목 모니터링 중",
        response.totalConditions(), response.monitoringStockCount());
    return response;
  }

  @GetMapping("/conditions/{stockCode}")
  @Operation(summary = "종목별 조건 조회", description = "특정 종목에 대한 가격 조건들을 조회합니다.")
  @ResponseStatus(code = HttpStatus.OK)
  public List<PriceConditionResponse> getConditionsByStock(@PathVariable String stockCode) {
    log.info("종목별 조건 조회 API 요청: {}", stockCode);

    List<PriceCondition> conditions = monitorPriceService.getConditions(stockCode);
    List<PriceConditionResponse> responses = conditions.stream()
        .map(PriceConditionResponse::from)
        .toList();

    log.info("종목 {} 조건 조회 성공: {}개", stockCode, responses.size());
    return responses;
  }

  @GetMapping("/stocks")
  @Operation(summary = "모니터링 중인 종목 목록 조회", description = "현재 모니터링 중인 종목 코드 목록을 조회합니다.")
  @ResponseStatus(code = HttpStatus.OK)
  public List<String> getMonitoringStocks() {
    log.info("모니터링 중인 종목 목록 조회 API 요청");

    List<String> stocks = monitorPriceService.getMonitoringStocks().stream().sorted().toList();

    log.info("모니터링 중인 종목 목록 조회 성공: {}개 종목", stocks.size());
    return stocks;
  }
}
