package com.kokimstocktrading.adapter.in.web.monitoring;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;

@Schema(description = "모니터링 상태 응답")
public record MonitoringStatusResponse(
    @Schema(description = "등록된 전체 조건 수")
    int totalConditions,

    @Schema(description = "모니터링 중인 종목 수")
    int monitoringStockCount,

    @Schema(description = "모니터링 중인 종목 코드 목록")
    Set<String> monitoringStocks,

    @Schema(description = "전체 가격 조건 목록")
    List<PriceConditionResponse> conditions
) {

}
