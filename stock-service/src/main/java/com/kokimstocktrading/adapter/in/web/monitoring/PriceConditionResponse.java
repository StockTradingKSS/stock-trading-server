package com.kokimstocktrading.adapter.in.web.monitoring;

import com.kokimstocktrading.domain.monitoring.PriceCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "가격 조건 응답")
public record PriceConditionResponse(
    @Schema(description = "조건 ID")
    UUID id,

    @Schema(description = "종목 코드", example = "005930")
    String stockCode,

    @Schema(description = "목표 가격", example = "75000")
    Long targetPrice,

    @Schema(description = "조건 설명", example = "삼성전자 75000원 도달")
    String description
) {

  public static PriceConditionResponse from(PriceCondition condition) {
    return new PriceConditionResponse(
        condition.getId(),
        condition.getStockCode(),
        condition.getTargetPrice(),
        condition.getDescription()
    );
  }
}
