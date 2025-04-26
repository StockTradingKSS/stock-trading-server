package com.KimStock.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Market {
    private String marketCode;           // 시장구분코드
    private String code;            // 코드
    private String name;           // 업종명
    private String group;
}
