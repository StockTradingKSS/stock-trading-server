package com.KimStock.adapter.out.kiwoom.dto;

import com.KimStock.domain.model.Market;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 업종코드 응답 DTO
 */
public record MarketCodeResponse(
    String return_msg,
    @JsonProperty(value = "list", access = JsonProperty.Access.WRITE_ONLY)
    List<MarketCode> marketCodeList
) {
    @Builder
    public MarketCodeResponse {
        marketCodeList = Objects.requireNonNullElse(marketCodeList, new ArrayList<>());
        return_msg = Objects.requireNonNullElse(return_msg, "null 값이 들어왔습니다.");
    }
    
    /**
     * 업종코드 아이템
     */
    public record MarketCode(
        String marketCode,           // 시장구분코드
        String code,            // 코드
        String name,           // 업종명
        String group          // 그룹
    ) {
        @Builder
        public MarketCode {
            marketCode = Objects.requireNonNullElse(marketCode, "");
            code = Objects.requireNonNullElse(code, "");
            name = Objects.requireNonNullElse(name, "");
            group = Objects.requireNonNullElse(group, "");
        }

        public Market mapToMarket(){
            return Market.builder()
                    .marketCode(marketCode)
                    .code(code)
                    .name(name)
                    .group(group)
                    .build();
        }
    }
}
