package com.KimStock.adapter.out.persistence.stockcandle.year;

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
@Table(name = "year_stock_candle")
public class YearStockCandleEntity {

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

    public static YearStockCandleEntity of(StockCandle stockCandle) {
        return YearStockCandleEntity.builder()
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
                .candleInterval(CandleInterval.YEAR)
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
