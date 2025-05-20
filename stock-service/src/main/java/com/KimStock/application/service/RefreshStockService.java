package com.KimStock.application.service;

import com.KimStock.application.port.in.RefreshStockUseCase;
import com.KimStock.application.port.out.LoadStockListPort;
import com.KimStock.application.port.out.SaveStockListPort;
import com.KimStock.domain.model.Stock;
import com.KimStock.domain.model.type.MarketType;
import com.common.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RefreshStockService implements RefreshStockUseCase {
    private final SaveStockListPort saveStockListPort;
    private final LoadStockListPort loadStockListPort;

    @Override
    public Mono<Boolean> refreshStock() {
        // KOSPI와 KOSDAQ 종목 정보들을 외부 API 를 통해 받아온다.
        // 받아온 종목 정보를 DB에 저장한다.
        Mono<List<Stock>> kosdaqMono = loadStockListPort.loadStockInfoListBy(MarketType.KOSDAQ, null, null)
                .doOnSuccess(saveStockListPort::saveStockList);

        Mono<List<Stock>> kospiMono = loadStockListPort.loadStockInfoListBy(MarketType.KOSPI, null, null)
                .doOnSuccess(saveStockListPort::saveStockList);

        return Mono.zip(kosdaqMono, kospiMono)
                .map(tuple -> true)
                .onErrorResume(e -> {
                    log.error("Failed to refresh stock information", e);
                    return Mono.just(false);
                });
    }
}
