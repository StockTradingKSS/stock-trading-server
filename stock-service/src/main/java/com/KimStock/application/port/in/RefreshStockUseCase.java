package com.KimStock.application.port.in;

import reactor.core.publisher.Mono;

public interface RefreshStockUseCase {

    Mono<Boolean> refreshStock();
}
