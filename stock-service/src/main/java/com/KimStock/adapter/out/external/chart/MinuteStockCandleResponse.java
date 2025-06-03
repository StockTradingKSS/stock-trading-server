package com.KimStock.adapter.out.external.chart;

import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
* 주식 차트 응답 DTO
*/
public record MinuteStockCandleResponse(
   @JsonProperty("stk_cd") String stockCode,
   @JsonProperty("stk_min_pole_chart_qry") List<ChartItem> chartItems,
   @JsonProperty("return_code") Integer returnCode,
   @JsonProperty("return_msg") String returnMsg
) {
   @Builder
   public MinuteStockCandleResponse {
       stockCode = Objects.requireNonNullElse(stockCode, "");
       chartItems = Objects.requireNonNullElse(chartItems, new ArrayList<>());
       returnCode = Objects.requireNonNullElse(returnCode, 0);
       returnMsg = Objects.requireNonNullElse(returnMsg, "");
   }
   
   /**
    * 차트 데이터 아이템
    */
   public record ChartItem(
       @JsonProperty("cur_prc") String currentPrice,          // 현재가
       @JsonProperty("trde_qty") String tradeQuantity,        // 거래량
       @JsonProperty("cntr_tm") String contractTime,          // 체결시간
       @JsonProperty("open_pric") String openPrice,           // 시가
       @JsonProperty("high_pric") String highPrice,           // 고가
       @JsonProperty("low_pric") String lowPrice,             // 저가
       @JsonProperty("upd_stkpc_tp") String updateStockPriceType,    // 상승하락구분
       @JsonProperty("upd_rt") String updateRate,             // 등락률
       @JsonProperty("bic_inds_tp") String bigIndustryType,   // 대업종구분
       @JsonProperty("sm_inds_tp") String smallIndustryType,  // 소업종구분
       @JsonProperty("stk_infr") String stockInfo,            // 종목정보
       @JsonProperty("upd_stkpc_event") String updateStockPriceEvent, // 상승하락이벤트
       @JsonProperty("pred_close_pric") String predictedClosePrice    // 예상종가
   ) {
       @Builder
       public ChartItem {
           currentPrice = Objects.requireNonNullElse(currentPrice, "");
           tradeQuantity = Objects.requireNonNullElse(tradeQuantity, "");
           contractTime = Objects.requireNonNullElse(contractTime, "");
           openPrice = Objects.requireNonNullElse(openPrice, "");
           highPrice = Objects.requireNonNullElse(highPrice, "");
           lowPrice = Objects.requireNonNullElse(lowPrice, "");
           updateStockPriceType = Objects.requireNonNullElse(updateStockPriceType, "");
           updateRate = Objects.requireNonNullElse(updateRate, "");
           bigIndustryType = Objects.requireNonNullElse(bigIndustryType, "");
           smallIndustryType = Objects.requireNonNullElse(smallIndustryType, "");
           stockInfo = Objects.requireNonNullElse(stockInfo, "");
           updateStockPriceEvent = Objects.requireNonNullElse(updateStockPriceEvent, "");
           predictedClosePrice = Objects.requireNonNullElse(predictedClosePrice, "");
       }

       public StockCandle mapToStockCandle(String stockCode, CandleInterval interval) {
           DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
           String price = currentPrice.startsWith("-") ? currentPrice.substring(1) : currentPrice;
           String open = openPrice.startsWith("-") ? openPrice.substring(1) : openPrice;
           String high = highPrice.startsWith("-") ? highPrice.substring(1) : highPrice;
           String low = lowPrice.startsWith("-") ? lowPrice.substring(1) : lowPrice;
           
           return StockCandle.builder()
                   .code(stockCode)
                   .candleInterval(interval)
                   .currentPrice(Long.parseLong(price))
                   .volume(Long.parseLong(tradeQuantity))
                   .openPrice(Long.parseLong(open))
                   .highPrice(Long.parseLong(high))
                   .lowPrice(Long.parseLong(low))
                   .closePrice(Long.parseLong(price))  // 현재가를 종가로 사용
                   .openTime(LocalDateTime.parse(contractTime, formatter))
                   .build();
       }
   }

    /**
     * 응답이 정상인지 확인
     * @return 정상 여부
     */
    public boolean isSuccess() {
        return returnCode != null && returnCode == 0;
    }
}
