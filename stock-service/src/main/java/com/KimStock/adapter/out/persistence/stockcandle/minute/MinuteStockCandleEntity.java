package com.KimStock.adapter.out.persistence.stockcandle.minute;

import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "minute_stock_candle")
public class MinuteStockCandleEntity {

    @Id
    private Long id;
    private String code;
    private Long currentPrice;
    private Long previousPrice;
    private Long volume;
    private Long openPrice;
    private Long highPrice;
    private Long lowPrice;
    private Long closePrice;
    private LocalDateTime openTime;

    public static MinuteStockCandleEntity of(StockCandle stockCandle) {
        return MinuteStockCandleEntity.builder()
                .code(stockCandle.getCode())
                .currentPrice(stockCandle.getCurrentPrice())
                .previousPrice(stockCandle.getPreviousPrice())
                .volume(stockCandle.getVolume())
                .openPrice(stockCandle.getOpenPrice())
                .highPrice(stockCandle.getHighPrice())
                .lowPrice(stockCandle.getLowPrice())
                .closePrice(stockCandle.getClosePrice())
                .openTime(stockCandle.getOpenTime())
                .build();
    }

    public StockCandle toDomain() {
        return StockCandle.builder()
                .code(this.code)
                .candleInterval(CandleInterval.MINUTE)
                .currentPrice(this.currentPrice)
                .previousPrice(this.previousPrice)
                .volume(this.volume)
                .openPrice(this.openPrice)
                .highPrice(this.highPrice)
                .lowPrice(this.lowPrice)
                .closePrice(this.closePrice)
                .openTime(this.openTime)
                .build();
    }
}
