package com.KimStock.adapter.out.external.account;

import com.KimStock.adapter.out.external.error.ClientErrorHandler;
import com.KimStock.adapter.out.external.kiwoom.auth.KiwoomAuthAdapter;
import com.KimStock.application.port.out.LoadAccountBalancePort;
import com.KimStock.domain.model.AccountBalance;
import com.common.ExternalSystemAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExternalSystemAdapter
@Slf4j
public class KiwoomLoadAccountBalanceAdapter implements LoadAccountBalancePort {
    private final WebClient webClient;
    private final KiwoomAuthAdapter kiwoomAuthAdapter;
    private final ClientErrorHandler clientErrorHandler;

    public KiwoomLoadAccountBalanceAdapter(
            @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient,
            KiwoomAuthAdapter kiwoomAuthAdapter, ClientErrorHandler clientErrorHandler) {

        this.webClient = kiwoomWebClient;
        this.kiwoomAuthAdapter = kiwoomAuthAdapter;
        this.clientErrorHandler = clientErrorHandler;
    }
    @Override
    public Mono<AccountBalance> loadAccountBalance() {
        AccountBalanceRequest request = AccountBalanceRequest.getDefaultRequest();

        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> loadAccountBalanceApi(token, request))
                .onErrorResume(e -> {
                    log.error("[Load Account Balance Error] : {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<AccountBalance> loadAccountBalanceApi(String token, AccountBalanceRequest request) {
        return webClient.post()
                .uri("/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "kt00018")
                .header("cont-yn", "N")
                .header("next-key", "")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(this::handleAccountBalanceResponse);
    }

    private Mono<AccountBalance> handleAccountBalanceResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(AccountBalanceResponse.class)
                    .map(AccountBalanceResponse::mapToAccountBalance)
                    .doOnSuccess(balance -> log.info("Account balance API call successful {}", balance));
        }

        return clientErrorHandler.handleErrorResponse(response, "Account balance request failed.");
    }
}
