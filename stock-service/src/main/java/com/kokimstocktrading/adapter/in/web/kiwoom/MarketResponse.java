package com.kokimstocktrading.adapter.in.web.kiwoom;

import com.kokimstocktrading.domain.model.Market;
import lombok.Builder;

import java.util.Objects;

public record MarketResponse(String marketCode,
                             String code,
                             String name,
                             String group) {
    @Builder
    public MarketResponse {
        marketCode = Objects.requireNonNullElse(marketCode, "");
        code = Objects.requireNonNullElse(code, "");
        name = Objects.requireNonNullElse(name, "");
        group = Objects.requireNonNullElse(group, "");
    }

    public static MarketResponse of(Market market) {
        return MarketResponse.builder()
                .marketCode(market.getMarketCode())
                .code(market.getCode())
                .name(market.getName())
                .group(market.getGroup()).build();
    }
}
