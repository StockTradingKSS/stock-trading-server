package com.kokimstocktrading.adapter.in.web.kiwoom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderCancelRequest(
    @Schema(description = "원주문번호", example = "1234567")
    @NotBlank(message = "원주문번호는 필수입니다")
    String orderNo,

    @Schema(description = "종목코드", example = "005930")
    @NotBlank(message = "종목코드는 필수입니다")
    String stockCode,

    @Schema(description = "취소수량(0일 경우 전량 취소)", example = "0")
    @Min(value = 0, message = "취소수량은 0 이상이어야 합니다")
    Integer quantity
) {

}
