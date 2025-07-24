package com.kokimstocktrading.adapter.out.external.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ClientErrorHandler {
    public <T> Mono<T> handleErrorResponse(ClientResponse response, String failInfoString) {
        return response.bodyToMono(String.class)
                .flatMap(errorBody -> {
                            String errorMessage = failInfoString + "Status : " + response.statusCode() + ", Body : " + errorBody;
                            return Mono.error(new RuntimeException(errorMessage));
                        }
                );
    }
}
