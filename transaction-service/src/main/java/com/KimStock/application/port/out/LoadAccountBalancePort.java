package com.KimStock.application.port.out;

import com.KimStock.domain.model.AccountBalance;
import reactor.core.publisher.Mono;

public interface LoadAccountBalancePort {
    Mono<AccountBalance> loadAccountBalance();
}
