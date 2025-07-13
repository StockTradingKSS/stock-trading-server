package com.KimStock.domain.model.type;

import lombok.Getter;

/**
 * 주식 주문 유형
 */
@Getter
public enum TradeType {
    LIMIT("0", "지정가"),    // 일반적인 지정가 주문
    MARKET("3", "시장가");   // 시장가 주문

    private final String code;    // 키움 API 주문 유형 코드
    private final String description;

    TradeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 코드로 TradeType 찾기
     *
     * @param code 찾을 코드
     * @return 해당 코드의 TradeType, 없으면 null
     */
    public static TradeType fromCode(String code) {
        if (code == null) {
            return LIMIT; // 기본값은 지정가
        }

        for (TradeType type : TradeType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }

        return LIMIT; // 일치하는 코드가 없으면 지정가 반환
    }
}
