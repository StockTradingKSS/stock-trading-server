package com.kokimstocktrading.adapter.in.web.kiwoom;

import com.kokimstocktrading.domain.model.AccountBalance;
import com.kokimstocktrading.domain.model.HoldingStock;
import lombok.Builder;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record AccountBalanceResponse(
        long totalPurchaseAmount,      // 총매입금액
        long totalEvaluationAmount,    // 총평가금액
        long totalEvaluationProfitLoss, // 총평가손익금액
        double totalProfitRate,        // 총수익률(%)
        long estimatedDepositAsset,    // 추정예탁자산
        long totalLoanAmount,          // 총대출금
        long totalCreditLoanAmount,    // 총융자금액
        long totalCreditLeaseAmount,   // 총대주금액
        List<HoldingStockResponse> holdingStocks // 보유종목 목록
) {
    @Builder
    public AccountBalanceResponse {
        totalPurchaseAmount = Objects.requireNonNullElse(totalPurchaseAmount, 0L);
        totalEvaluationAmount = Objects.requireNonNullElse(totalEvaluationAmount, 0L);
        totalEvaluationProfitLoss = Objects.requireNonNullElse(totalEvaluationProfitLoss, 0L);
        totalProfitRate = Objects.requireNonNullElse(totalProfitRate, 0.0);
        estimatedDepositAsset = Objects.requireNonNullElse(estimatedDepositAsset, 0L);
        totalLoanAmount = Objects.requireNonNullElse(totalLoanAmount, 0L);
        totalCreditLoanAmount = Objects.requireNonNullElse(totalCreditLoanAmount, 0L);
        totalCreditLeaseAmount = Objects.requireNonNullElse(totalCreditLeaseAmount, 0L);
        holdingStocks = Objects.requireNonNullElse(holdingStocks, List.of());
    }

    public static AccountBalanceResponse of(AccountBalance accountBalance) {
        return AccountBalanceResponse.builder()
                .totalPurchaseAmount(accountBalance.getTotalPurchaseAmount())
                .totalEvaluationAmount(accountBalance.getTotalEvaluationAmount())
                .totalEvaluationProfitLoss(accountBalance.getTotalEvaluationProfitLoss())
                .totalProfitRate(accountBalance.getTotalProfitRate())
                .estimatedDepositAsset(accountBalance.getEstimatedDepositAsset())
                .totalLoanAmount(accountBalance.getTotalLoanAmount())
                .totalCreditLoanAmount(accountBalance.getTotalCreditLoanAmount())
                .totalCreditLeaseAmount(accountBalance.getTotalCreditLeaseAmount())
                .holdingStocks(accountBalance.getHoldingStocks().stream()
                        .map(HoldingStockResponse::of)
                        .collect(Collectors.toList()))
                .build();
    }

    public record HoldingStockResponse(
            String code,                // 종목코드
            String name,                // 종목명
            int quantity,               // 보유수량
            int tradeableQuantity,      // 매매가능수량
            long purchasePrice,         // 매입가격(총액)
            int avgPrice,               // 평균단가
            long currentPrice,          // 현재가
            long evaluationAmount,      // 평가금액
            long profitLoss,            // 평가손익
            double profitLossRate,      // 수익률(%)
            double possessionRate,      // 보유비중(%)
            String creditType,          // 신용구분
            String creditTypeName,      // 신용구분명
            String creditLoanDate       // 대출일
    ) {
        @Builder
        public HoldingStockResponse {
            code = Objects.requireNonNullElse(code, "");
            name = Objects.requireNonNullElse(name, "");
            creditType = Objects.requireNonNullElse(creditType, "");
            creditTypeName = Objects.requireNonNullElse(creditTypeName, "");
            creditLoanDate = Objects.requireNonNullElse(creditLoanDate, "");
        }

        public static HoldingStockResponse of(HoldingStock holdingStock) {
            return HoldingStockResponse.builder()
                    .code(holdingStock.getCode())
                    .name(holdingStock.getName())
                    .quantity(holdingStock.getQuantity())
                    .tradeableQuantity(holdingStock.getTradeableQuantity())
                    .purchasePrice(holdingStock.getPurchasePrice())
                    .avgPrice(holdingStock.getAvgPrice())
                    .currentPrice(holdingStock.getCurrentPrice())
                    .evaluationAmount(holdingStock.getEvaluationAmount())
                    .profitLoss(holdingStock.getProfitLoss())
                    .profitLossRate(holdingStock.getProfitLossRate())
                    .possessionRate(holdingStock.getPossessionRate())
                    .creditType(holdingStock.getCreditType())
                    .creditTypeName(holdingStock.getCreditTypeName())
                    .creditLoanDate(holdingStock.getCreditLoanDate())
                    .build();
        }
    }
}
