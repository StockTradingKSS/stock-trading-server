package com.kokimstocktrading.application.port.out;

import com.kokimstocktrading.domain.model.AccountBalance;
import reactor.core.publisher.Mono;

public interface LoadAccountBalancePort {
    Mono<AccountBalance> loadAccountBalance();
}
