package com.KimStock.adapter.out.kiwoom.dto;

import com.KimStock.domain.model.Stock;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 종목 정보 응답 DTO
 */
public record StockInfoResponse(
    @JsonProperty("return_msg") String returnMsg,
    @JsonProperty("return_code") Integer returnCode,
    @JsonProperty("list") List<StockItem> stockItems
) {
    @Builder
    public StockInfoResponse {
        returnMsg = Objects.requireNonNullElse(returnMsg, "");
        returnCode = Objects.requireNonNullElse(returnCode, 0);
        stockItems = Objects.requireNonNullElse(stockItems, new ArrayList<>());
    }
    
    /**
     * 종목 정보 아이템
     */
    public record StockItem(
        String code,                           // 종목코드
        String name,                           // 종목명
        @JsonProperty("listCount") String listCount,    // 상장주식수
        @JsonProperty("auditInfo") String auditInfo,    // 감사정보
        @JsonProperty("regDay") String regDay,          // 등록일
        @JsonProperty("lastPrice") String lastPrice,    // 현재가
        String state,                          // 종목 상태
        @JsonProperty("marketCode") String marketCode,  // 시장코드
        @JsonProperty("marketName") String marketName,  // 시장명
        @JsonProperty("upName") String upName,          // 업종명
        @JsonProperty("upSizeName") String upSizeName,  // 규모명
        @JsonProperty("companyClassName") String companyClassName, // 기업형태
        @JsonProperty("orderWarning") String orderWarning,        // 주문경고
        @JsonProperty("nxtEnable") String nxtEnable              // 다음페이지유무
    ) {
        @Builder
        public StockItem {
            code = Objects.requireNonNullElse(code, "");
            name = Objects.requireNonNullElse(name, "");
            listCount = Objects.requireNonNullElse(listCount, "");
            auditInfo = Objects.requireNonNullElse(auditInfo, "");
            regDay = Objects.requireNonNullElse(regDay, "");
            lastPrice = Objects.requireNonNullElse(lastPrice, "");
            state = Objects.requireNonNullElse(state, "");
            marketCode = Objects.requireNonNullElse(marketCode, "");
            marketName = Objects.requireNonNullElse(marketName, "");
            upName = Objects.requireNonNullElse(upName, "");
            upSizeName = Objects.requireNonNullElse(upSizeName, "");
            companyClassName = Objects.requireNonNullElse(companyClassName, "");
            orderWarning = Objects.requireNonNullElse(orderWarning, "");
            nxtEnable = Objects.requireNonNullElse(nxtEnable, "");
        }

        public Stock mapToStock(){
            return Stock.builder()
                    .code(code)
                    .name(name)
                    .listCount(Long.valueOf(listCount))
                    .auditInfo(auditInfo)
                    .regDay(regDay)
                    .state(state)
                    .marketCode(marketCode)
                    .marketName(marketName)
                    .upName(upName)
                    .upSizeName(upSizeName)
                    .companyClassName(companyClassName)
                    .orderWarning(orderWarning)
                    .build();
        }
    }
}
