package com.KimStock.adapter.out.external.chart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class TossInvestChartResponse {

    @JsonProperty("result")
    private Result result;

    @Getter
    public static class Result {
        @JsonProperty("code")
        private String code;

        @JsonProperty("nextDateTime")
        private OffsetDateTime nextDateTime;  // LocalDateTime -> OffsetDateTime

        @JsonProperty("exchangeRate")
        private Integer exchangeRate;

        @JsonProperty("exchange")
        private String exchange;

        @JsonProperty("candles")
        private List<Candle> candles;
    }

    @Getter
    public static class Candle {
        @JsonProperty("dt")
        private OffsetDateTime dt;  // LocalDateTime -> OffsetDateTime

        @JsonProperty("base")
        private Long base;

        @JsonProperty("open")
        private Long open;

        @JsonProperty("high")
        private Long high;

        @JsonProperty("low")
        private Long low;

        @JsonProperty("close")
        private Long close;

        @JsonProperty("volume")
        private Long volume;

        @JsonProperty("amount")
        private Long amount;
    }
}
