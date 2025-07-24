package com.kokimstocktrading.adapter.in.web.kiwoom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderModifyRequest(
        @Schema(description = "원주문번호", example = "1234567")
        @NotBlank(message = "원주문번호는 필수입니다")
        String orderNo,

        @Schema(description = "종목코드", example = "005930")
        @NotBlank(message = "종목코드는 필수입니다")
        String stockCode,

        @Schema(description = "정정수량", example = "5")
        @NotNull(message = "정정수량은 필수입니다")
        @Min(value = 1, message = "정정수량은 1 이상이어야 합니다")
        Integer quantity,

        @Schema(description = "정정단가", example = "76000")
        @NotNull(message = "정정단가는 필수입니다")
        Double price
) {
}
