package com.KimStock.adapter.out.persistence.stockcandle;

import com.KimStock.adapter.out.persistence.stockcandle.day.*;
import com.KimStock.adapter.out.persistence.stockcandle.minute.*;
import com.KimStock.adapter.out.persistence.stockcandle.month.*;
import com.KimStock.adapter.out.persistence.stockcandle.week.*;
import com.KimStock.adapter.out.persistence.stockcandle.year.*;
import com.KimStock.domain.model.StockCandle;
import com.KimStock.domain.model.type.CandleInterval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockCandlePersistenceAdapter {

    private final DayStockCandleRepository dayRepository;
    private final MinuteStockCandleRepository minuteRepository;
    private final WeekStockCandleRepository weekRepository;
    private final MonthStockCandleRepository monthRepository;
    private final YearStockCandleRepository yearRepository;
    
    private final DayStockCandleBulkRepositoryImpl dayBulkRepository;
    private final MinuteStockCandleBulkRepositoryImpl minuteBulkRepository;
    private final WeekStockCandleBulkRepositoryImpl weekBulkRepository;
    private final MonthStockCandleBulkRepositoryImpl monthBulkRepository;
    private final YearStockCandleBulkRepositoryImpl yearBulkRepository;

    public Mono<Void> bulkSaveStockCandles(List<StockCandle> stockCandles, CandleInterval interval) {
        return switch (interval) {
            case DAY -> {
                List<DayStockCandleEntity> entities = stockCandles.stream()
                        .map(DayStockCandleEntity::of)
                        .toList();
                yield dayBulkRepository.bulkInsert(entities);
            }
            case MINUTE -> {
                List<MinuteStockCandleEntity> entities = stockCandles.stream()
                        .map(MinuteStockCandleEntity::of)
                        .toList();
                yield minuteBulkRepository.bulkInsert(entities);
            }
            case WEEK -> {
                List<WeekStockCandleEntity> entities = stockCandles.stream()
                        .map(WeekStockCandleEntity::of)
                        .toList();
                yield weekBulkRepository.bulkInsert(entities);
            }
            case MONTH -> {
                List<MonthStockCandleEntity> entities = stockCandles.stream()
                        .map(MonthStockCandleEntity::of)
                        .toList();
                yield monthBulkRepository.bulkInsert(entities);
            }
            case YEAR -> {
                List<YearStockCandleEntity> entities = stockCandles.stream()
                        .map(YearStockCandleEntity::of)
                        .toList();
                yield yearBulkRepository.bulkInsert(entities);
            }
            default -> Mono.empty();
        };
    }

    public Flux<StockCandle> findStockCandles(String code, CandleInterval interval) {
        return switch (interval) {
            case DAY -> dayRepository.findByCodeOrderByOpenTimeDesc(code)
                    .map(DayStockCandleEntity::toDomain);
            case MINUTE -> minuteRepository.findByCodeOrderByOpenTimeDesc(code)
                    .map(MinuteStockCandleEntity::toDomain);
            case WEEK -> weekRepository.findByCodeOrderByOpenTimeDesc(code)
                    .map(WeekStockCandleEntity::toDomain);
            case MONTH -> monthRepository.findByCodeOrderByOpenTimeDesc(code)
                    .map(MonthStockCandleEntity::toDomain);
            case YEAR -> yearRepository.findByCodeOrderByOpenTimeDesc(code)
                    .map(YearStockCandleEntity::toDomain);
            default -> Flux.empty();
        };
    }
}
