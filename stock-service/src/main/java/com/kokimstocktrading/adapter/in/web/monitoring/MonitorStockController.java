package com.kokimstocktrading.adapter.in.web.monitoring;

import com.common.WebAdapter;
import com.kokimstocktrading.application.condition.port.in.RegisterMovingAverageCommand;
import com.kokimstocktrading.application.condition.port.in.RegisterTradingConditionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebAdapter
@RestController
@RequestMapping("/api/monitor")
@Tag(name = "Monitor API", description = "Monitor 관련 API")
@RequiredArgsConstructor
@Slf4j
public class MonitorStockController {
    private final RegisterTradingConditionUseCase registerTradingConditionUseCase;

    @PostMapping("/refresh")
    @Operation(summary = "조건 감시", description = "이평선 조건 감시")
    public void refreshStock(@RequestBody RegisterMovingAverageCommand registerMovingAverageCommand) {
        registerTradingConditionUseCase.registerMovingAverageCondition(registerMovingAverageCommand);
    }
}
