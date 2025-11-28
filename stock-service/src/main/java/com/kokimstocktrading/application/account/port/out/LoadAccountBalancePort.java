package com.kokimstocktrading.application.account.port.out;

import com.kokimstocktrading.domain.account.AccountBalance;
import reactor.core.publisher.Mono;

public interface LoadAccountBalancePort {

  Mono<AccountBalance> loadAccountBalance();
}
