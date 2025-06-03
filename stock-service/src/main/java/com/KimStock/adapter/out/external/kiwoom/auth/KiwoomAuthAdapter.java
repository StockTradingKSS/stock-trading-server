package com.KimStock.adapter.out.external.kiwoom.auth;

import com.common.ExternalSystemAdapter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

@ExternalSystemAdapter
@Slf4j
public class KiwoomAuthAdapter {
    private final WebClient kiwoomWebClient;
    private final String appKey;
    private final String appSecret;
    // 토큰 정보 캐싱
    private final AtomicReference<String> accessToken = new AtomicReference<>("");
    private final AtomicReference<Instant> tokenExpiration = new AtomicReference<>(Instant.now());

    public KiwoomAuthAdapter(
            @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient,
            @Value("${kiwoom.api.app-key:}") String appKey,
            @Value("${kiwoom.api.app-secret:}") String appSecret) {

        this.appKey = appKey;
        this.appSecret = appSecret;
        // API 호출용 WebClient
        this.kiwoomWebClient = kiwoomWebClient;
    }

    /**
     * 초기화 - 앱 시작 시 액세스 토큰 발급
     */
    @PostConstruct
    public void init() {
        if (!appKey.isEmpty() && !appSecret.isEmpty()) {
            requestAccessToken()
                    .subscribe(
                            response -> log.info("Initial access token acquired successfully"),
                            error -> log.error("Failed to acquire initial access token: {}", error.getMessage())
                    );
        } else {
            log.warn("App key or secret key is not configured. Token will not be acquired automatically.");
        }
    }

    /**
     * OAuth 토큰 발급
     * @return 토큰 응답
     */
    public Mono<OAuthTokenResponse> requestAccessToken() {
        log.info("Requesting access token");
        OAuthTokenRequest oAuthTokenRequest = OAuthTokenRequest.builder()
                .appkey(appKey)
                .secretkey(appSecret)
                .grantType("client_credentials")
                .build();


        return kiwoomWebClient.post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(oAuthTokenRequest)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(OAuthTokenResponse.class)
                                .doOnSuccess(tokenResponse -> {
                                    log.info("Access token response received with code: {}, message: {}",
                                            tokenResponse.return_code(), tokenResponse.return_msg());
                                    updateTokenInfo(tokenResponse);
                                });
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Token request failed. Status: {}, Body: {}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(
                                            new RuntimeException("Failed to get token. Status: " +
                                                    response.statusCode() + ", Body: " + errorBody));
                                });
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error getting access token: {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    /**
     * 토큰 폐기
     * @return 폐기 결과
     */
    public Mono<Void> revokeToken() {
        log.info("Revoking access token");

        if (accessToken.get().isEmpty()) {
            return Mono.error(new IllegalStateException("No access token available to revoke"));
        }

        TokenRevokeRequest tokenRevokeRequest = TokenRevokeRequest.builder()
                .appkey(appKey)
                .secretkey(appSecret)
                .token(accessToken.get()).build();

        return kiwoomWebClient.post()
                .uri("/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tokenRevokeRequest)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(String.class)
                                .doOnSuccess(body -> {
                                    log.info("Token revoked successfully. Response: {}", body);
                                    accessToken.set("");
                                    tokenExpiration.set(Instant.now());
                                })
                                .then();
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Token revoke failed. Status: {}, Body: {}",
                                            response.statusCode(), errorBody);
                                    return Mono.error(
                                            new RuntimeException("Failed to revoke token. Status: " +
                                                    response.statusCode() + ", Body: " + errorBody));
                                });
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error revoking token: {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    /**
     * 토큰 정보 업데이트
     * @param response 토큰 응답
     */
    private void updateTokenInfo(OAuthTokenResponse response) {
        // 정상 응답인 경우에만 토큰 업데이트
        if (response.return_code() == 0) {
            accessToken.set(response.token());

            // 만료 시간 파싱 (yyyyMMddHHmmss 형식)
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                LocalDateTime expiresDateTime = LocalDateTime.parse(response.expires_dt(), formatter);
                ZoneId zoneId = ZoneId.systemDefault();
                Instant expiresInstant = expiresDateTime.atZone(zoneId).toInstant();
                tokenExpiration.set(expiresInstant);

                log.info("Token updated, expires at: {}", tokenExpiration.get());
            } catch (Exception e) {
                log.error("Failed to parse expires_dt: {}", e.getMessage());
                // 파싱 실패 시 기본값으로 1시간 설정
                tokenExpiration.set(Instant.now().plus(Duration.ofHours(1)));
            }
        } else {
            log.error("Token request failed: {} - {}", response.return_code(), response.return_msg());
        }
    }

    /**
     * 토큰이 만료되었는지 확인하고 필요시 갱신
     * @return 유효한 토큰 반환
     */
    public Mono<String> getValidToken() {
        if (accessToken.get().isEmpty()) {
            log.info("No access token available, requesting new token");
            return requestAccessToken()
                    .map(OAuthTokenResponse::token);
        }

        if (Instant.now().isAfter(tokenExpiration.get().minus(Duration.ofMinutes(5)))) {
            log.info("Token expires soon, requesting new token");
            return requestAccessToken()
                    .map(OAuthTokenResponse::token)
                    .onErrorResume(e -> {
                        log.error("Failed to get new token: {}", e.getMessage());
                        return Mono.error(new IllegalStateException("Failed to get new token", e));
                    });
        }

        return Mono.just(accessToken.get());
    }
}
