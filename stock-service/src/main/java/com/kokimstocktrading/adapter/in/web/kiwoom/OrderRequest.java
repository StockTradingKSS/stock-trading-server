package com.kokimstocktrading.adapter.in.web.kiwoom;

import com.kokimstocktrading.domain.order.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(
        @Schema(description = "종목코드", example = "005930")
        @NotBlank(message = "종목코드는 필수입니다")
        String stockCode,

        @Schema(description = "주문수량", example = "10")
        @NotNull(message = "주문수량은 필수입니다")
        @Min(value = 1, message = "주문수량은 1 이상이어야 합니다")
        Integer quantity,

        @Schema(description = "주문단가 (시장가 주문일 경우 입력하지 않음)", example = "75000")
        Double price,

        @Schema(description = "주문유형 (LIMIT:지정가, MARKET:시장가)", example = "LIMIT")
        TradeType tradeType
) {
    // 주문 유형이 null일 경우 기본값을 지정가로 설정
    public TradeType tradeType() {
        return tradeType != null ? tradeType : TradeType.LIMIT;
    }


    @Override
    public String toString() {
        return "OrderRequest{" +
                "stockCode='" + stockCode + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", tradeType=" + tradeType +
                '}';
    }
}
