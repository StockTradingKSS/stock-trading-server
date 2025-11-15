package com.kokimstocktrading.adapter.out.external.kakao;

import com.kokimstocktrading.adapter.out.external.kakao.dto.KakaoTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 카카오 토큰 관리 서비스
 * Access Token 자동 갱신 기능 제공
 */
@Service
@Slf4j
public class KakaoTokenService {

    @Value("${kakao.api.auth-url}")
    private String authUrl;

    @Value("${kakao.api.rest-api-key}")
    private String restApiKey;

    @Value("${kakao.api.refresh-token}")
    private String refreshToken;

    private final WebClient webClient;
    private final AtomicReference<String> cachedAccessToken = new AtomicReference<>();
    private final AtomicReference<Instant> tokenExpiryTime = new AtomicReference<>();

    public KakaoTokenService() {
        this.webClient = WebClient.builder().build();
    }

    /**
     * 유효한 Access Token 반환 (필요 시 자동 갱신)
     */
    public Mono<String> getValidAccessToken() {
        // 캐시된 토큰이 있고 만료되지 않았으면 반환
        if (cachedAccessToken.get() != null && isTokenValid()) {
            log.debug("캐시된 Access Token 사용");
            return Mono.just(cachedAccessToken.get());
        }

        // 토큰 갱신 필요
        log.info("Access Token 갱신 시작");
        return refreshAccessToken();
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급
     */
    private Mono<String> refreshAccessToken() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", restApiKey);
        params.add("refresh_token", refreshToken);

        return webClient.post()
                .uri(authUrl + "/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .doOnSuccess(response -> {
                    cachedAccessToken.set(response.getAccessToken());
                    // 만료 시간 설정 (현재 시간 + expires_in - 버퍼 60초)
                    tokenExpiryTime.set(Instant.now().plusSeconds(response.getExpiresIn() - 60));
                    log.info("Access Token 갱신 성공, 만료 시간: {}", tokenExpiryTime.get());
                })
                .doOnError(error -> log.error("Access Token 갱신 실패", error))
                .map(KakaoTokenResponse::getAccessToken);
    }

    /**
     * 토큰 유효성 확인
     */
    private boolean isTokenValid() {
        Instant expiry = tokenExpiryTime.get();
        if (expiry == null) {
            return false;
        }
        return Instant.now().isBefore(expiry);
    }
}
