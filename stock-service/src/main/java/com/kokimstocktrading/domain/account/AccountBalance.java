package com.kokimstocktrading.domain.account;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AccountBalance {

  private long totalPurchaseAmount;  // 총매입금액
  private long totalEvaluationAmount;  // 총평가금액
  private long totalEvaluationProfitLoss;  // 총평가손익금액
  private double totalProfitRate;  // 총수익률(%)
  private long estimatedDepositAsset;  // 추정예탁자산
  private long totalLoanAmount;  // 총대출금
  private long totalCreditLoanAmount;  // 총융자금액
  private long totalCreditLeaseAmount;  // 총대주금액
  private List<HoldingStock> holdingStocks;  // 보유종목 목록
}
