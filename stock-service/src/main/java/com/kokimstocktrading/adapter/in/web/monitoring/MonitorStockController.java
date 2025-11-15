package com.kokimstocktrading.adapter.in.web.monitoring;

import com.common.WebAdapter;
import com.kokimstocktrading.application.condition.port.in.RegisterMovingAverageCommand;
import com.kokimstocktrading.application.condition.port.in.RegisterTradingConditionUseCase;
import com.kokimstocktrading.application.condition.port.in.RegisterTrendLineCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@WebAdapter
@RestController
@RequestMapping("/api/monitor")
@Tag(name = "Monitor API", description = "Monitor 관련 API")
@RequiredArgsConstructor
@Slf4j
public class MonitorStockController {
    private final RegisterTradingConditionUseCase registerTradingConditionUseCase;

    @PostMapping("/moving-average")
    @Operation(summary = "이평선 조건 등록", description = "이평선 조건을 등록합니다. DB에 저장하고 거래 시간이면 모니터링을 시작합니다.")
    public void registerMovingAverageCondition(@RequestBody RegisterMovingAverageCommand command) {
        log.info("이평선 조건 등록 API 요청: {}", command);
        registerTradingConditionUseCase.registerMovingAverageCondition(command)
                .doOnSuccess(condition -> log.info("이평선 조건 등록 성공: {}", condition.getId()))
                .doOnError(error -> log.error("이평선 조건 등록 실패", error))
                .subscribe();
    }

    @PostMapping("/trend-line")
    @Operation(summary = "추세선 조건 등록", description = "추세선 조건을 등록합니다. DB에 저장하고 거래 시간이면 모니터링을 시작합니다.")
    public void registerTrendLineCondition(@RequestBody RegisterTrendLineCommand command) {
        log.info("추세선 조건 등록 API 요청: {}", command);
        registerTradingConditionUseCase.registerTrendLineCondition(command)
                .doOnSuccess(condition -> log.info("추세선 조건 등록 성공: {}", condition.getId()))
                .doOnError(error -> log.error("추세선 조건 등록 실패", error))
                .subscribe();
    }

    @DeleteMapping("/{conditionId}")
    @Operation(summary = "조건 삭제", description = "등록된 조건을 삭제합니다. DB에서 비활성화하고 모니터링도 중지합니다.")
    public void deleteCondition(@PathVariable UUID conditionId) {
        log.info("조건 삭제 API 요청: {}", conditionId);
        registerTradingConditionUseCase.deleteCondition(conditionId)
                .doOnSuccess(unused -> log.info("조건 삭제 성공: {}", conditionId))
                .doOnError(error -> log.error("조건 삭제 실패: {}", conditionId, error))
                .subscribe();
    }
}
