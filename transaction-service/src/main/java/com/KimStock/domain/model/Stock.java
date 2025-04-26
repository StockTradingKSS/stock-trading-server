package com.KimStock.domain.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Stock {
    private String code;                           // 종목코드
    private String name;                           // 종목명
    private String listCount;                      // 상장주식수
    private String auditInfo;    // 감사정보
    private String regDay;          // 등록일
    private String state;       // 종목 상태
    private String marketCode;  // 시장코드
    private String marketName;  // 시장명
    private String upName;          // 업종명
    private String upSizeName;  // 규모명
    private String companyClassName; // 기업형태
    private String orderWarning; // 주문 경고
}


