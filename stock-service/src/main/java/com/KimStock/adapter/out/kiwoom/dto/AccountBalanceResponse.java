package com.KimStock.adapter.out.kiwoom.dto;

import com.KimStock.domain.model.AccountBalance;
import com.KimStock.domain.model.HoldingStock;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Collectors;

public record AccountBalanceResponse(
    @JsonProperty("tot_pur_amt") String totalPurchaseAmount,  // 총매입금액
    @JsonProperty("tot_evlt_amt") String totalEvaluationAmount,  // 총평가금액
    @JsonProperty("tot_evlt_pl") String totalEvaluationProfitLoss,  // 총평가손익금액
    @JsonProperty("tot_prft_rt") String totalProfitRate,  // 총수익률(%)
    @JsonProperty("prsm_dpst_aset_amt") String estimatedDepositAsset,  // 추정예탁자산
    @JsonProperty("tot_loan_amt") String totalLoanAmount,  // 총대출금
    @JsonProperty("tot_crd_loan_amt") String totalCreditLoanAmount,  // 총융자금액
    @JsonProperty("tot_crd_ls_amt") String totalCreditLeaseAmount,  // 총대주금액
    @JsonProperty("acnt_evlt_remn_indv_tot") List<StockHoldingResponse> stockHoldings,  // 계좌평가잔고개별합산
    @JsonProperty("return_code") int returnCode,
    @JsonProperty("return_msg") String returnMessage
) {
    public AccountBalance mapToAccountBalance() {
        List<HoldingStock> holdingStocks = stockHoldings == null
            ? List.of() 
            : stockHoldings.stream()
                .map(StockHoldingResponse::mapToHoldingStock)
                .collect(Collectors.toList());
        
        return AccountBalance.builder()
                .totalPurchaseAmount(Long.parseLong(totalPurchaseAmount))
                .totalEvaluationAmount(Long.parseLong(totalEvaluationAmount))
                .totalEvaluationProfitLoss(Long.parseLong(totalEvaluationProfitLoss))
                .totalProfitRate(Double.parseDouble(totalProfitRate))
                .estimatedDepositAsset(Long.parseLong(estimatedDepositAsset))
                .totalLoanAmount(Long.parseLong(totalLoanAmount))
                .totalCreditLoanAmount(Long.parseLong(totalCreditLoanAmount))
                .totalCreditLeaseAmount(Long.parseLong(totalCreditLeaseAmount))
                .holdingStocks(holdingStocks)
                .build();
    }
}
