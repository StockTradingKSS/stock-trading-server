package com.kokimstocktrading.adapter.in.web.kiwoom;

import com.kokimstocktrading.domain.model.RealTimeQuote;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 실시간 주식 시세 응답 DTO
 */
public record RealTimeQuoteResponse(
        @Schema(description = "종목코드", example = "005930")
        String stockCode,

        @Schema(description = "현재가", example = "20800")
        String currentPrice,

        @Schema(description = "전일대비", example = "-50")
        String priceChange,

        @Schema(description = "등락율", example = "-0.24")
        String changeRate,

        @Schema(description = "최우선 매도호가", example = "20800")
        String askPrice,

        @Schema(description = "최우선 매수호가", example = "20700")
        String bidPrice,

        @Schema(description = "거래량", example = "+82")
        String tradingVolume,

        @Schema(description = "누적거래량", example = "30379732")
        String accumulatedVolume,

        @Schema(description = "누적거래대금", example = "632640")
        String accumulatedAmount,

        @Schema(description = "시가", example = "20850")
        String openPrice,

        @Schema(description = "고가", example = "21150")
        String highPrice,

        @Schema(description = "저가", example = "20450")
        String lowPrice,

        @Schema(description = "체결시간", example = "165208")
        String tradeTime
) {
    /**
     * 도메인 모델에서 응답 DTO로 변환
     */
    public static RealTimeQuoteResponse from(RealTimeQuote quote) {
        return new RealTimeQuoteResponse(
                quote.item(),
                quote.currentPrice(),
                quote.priceChange(),
                quote.changeRate(),
                quote.askPrice(),
                quote.bidPrice(),
                quote.tradingVolume(),
                quote.accumulatedVolume(),
                quote.accumulatedAmount(),
                quote.openPrice(),
                quote.highPrice(),
                quote.lowPrice(),
                quote.tradeTime()
        );
    }
}
