package com.KimStock.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class HoldingStock {
    private String code;         // 종목코드
    private String name;         // 종목명
    private int quantity;        // 보유수량
    private int tradeableQuantity;  // 매매가능수량
    private long purchasePrice;  // 매입가격(총액)
    private int avgPrice;        // 평균단가
    private long currentPrice;   // 현재가
    private long evaluationAmount; // 평가금액
    private long profitLoss;     // 평가손익
    private double profitLossRate; // 수익률(%)
    private double possessionRate; // 보유비중(%)
    private String creditType;   // 신용구분
    private String creditTypeName; // 신용구분명
    private String creditLoanDate; // 대출일
}
