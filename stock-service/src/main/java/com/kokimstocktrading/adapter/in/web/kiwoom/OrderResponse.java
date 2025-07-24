package com.kokimstocktrading.adapter.in.web.kiwoom;

import com.kokimstocktrading.domain.model.OrderResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrderResponse(
        @Schema(description = "주문번호", example = "1234567")
        String orderNo,

        @Schema(description = "원주문번호 (정정/취소의 경우)", example = "7654321")
        String originalOrderNo,

        @Schema(description = "종목코드", example = "005930")
        String stockCode,

        @Schema(description = "주문구분", example = "BUY", allowableValues = {"BUY", "SELL", "MODIFY", "CANCEL"})
        String orderType,

        @Schema(description = "주문수량", example = "10")
        int quantity,

        @Schema(description = "시장구분", example = "KRX")
        String marketType,

        @Schema(description = "결과 메시지", example = "정상적으로 처리되었습니다")
        String resultMessage
) {
    public static OrderResponse from(OrderResult orderResult) {
        return new OrderResponse(
                orderResult.getOrderNo(),
                orderResult.getOriginalOrderNo(),
                orderResult.getStockCode(),
                orderResult.getOrderType().toString(),
                orderResult.getQuantity(),
                orderResult.getMarketType(),
                orderResult.getResultMessage()
        );
    }
}
