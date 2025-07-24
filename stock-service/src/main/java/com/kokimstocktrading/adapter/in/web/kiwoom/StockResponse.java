package com.kokimstocktrading.adapter.in.web.kiwoom;

import com.kokimstocktrading.domain.model.Stock;
import lombok.Builder;

import java.util.Objects;

public record StockResponse(
        String code,
        String name,
        Long listCount,
        String auditInfo,
        String regDay,
        String state,
        String marketCode,
        String marketName,
        String upName,
        String upSizeName,
        String companyClassName,
        String orderWarning
) {
    @Builder
    public StockResponse {
        code = Objects.requireNonNullElse(code, "");
        name = Objects.requireNonNullElse(name, "");
        listCount = Objects.requireNonNullElse(listCount, 0L);
        auditInfo = Objects.requireNonNullElse(auditInfo, "");
        regDay = Objects.requireNonNullElse(regDay, "");
        state = Objects.requireNonNullElse(state, "");
        marketCode = Objects.requireNonNullElse(marketCode, "");
        marketName = Objects.requireNonNullElse(marketName, "");
        upName = Objects.requireNonNullElse(upName, "");
        upSizeName = Objects.requireNonNullElse(upSizeName, "");
        companyClassName = Objects.requireNonNullElse(companyClassName, "");
        orderWarning = Objects.requireNonNullElse(orderWarning, "");
    }

    public static StockResponse of(Stock stock) {
        return StockResponse.builder()
                .code(stock.getCode())
                .name(stock.getName())
                .listCount(stock.getListCount())
                .auditInfo(stock.getAuditInfo())
                .regDay(stock.getRegDay())
                .state(stock.getState())
                .marketCode(stock.getMarketCode())
                .marketName(stock.getMarketName())
                .upName(stock.getUpName())
                .upSizeName(stock.getUpSizeName())
                .companyClassName(stock.getCompanyClassName())
                .orderWarning(stock.getOrderWarning())
                .build();
    }
}
