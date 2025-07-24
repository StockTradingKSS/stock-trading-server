package com.kokimstocktrading.adapter.in.web.kiwoom;

import com.kokimstocktrading.adapter.out.external.kiwoom.auth.KiwoomAuthAdapter;
import com.kokimstocktrading.adapter.out.external.kiwoom.auth.OAuthTokenResponse;
import com.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@WebAdapter
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kiwoom/auth")
@Tag(name = "Kiwoom Auth", description = "키움증권 인증 관련 API")
public class KiwoomAuthController {

    private final KiwoomAuthAdapter kiwoomAuthAdapter;

    @PostMapping("/token")
    @Operation(summary = "액세스 토큰 발급", description = "키움증권 API 액세스 토큰을 발급합니다.")
    public Mono<ResponseEntity<OAuthTokenResponse>> getAccessToken() {
        return kiwoomAuthAdapter.requestAccessToken()
                .map(tokenResponse -> {
                    log.info("Access token issued successfully");
                    return ResponseEntity.ok(tokenResponse);
                })
                .onErrorResume(e -> {
                    log.error("Error issuing access token: {}", e.getMessage(), e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to issue access token: " + e.getMessage()));
                });
    }

    @PostMapping("/revoke")
    @Operation(summary = "토큰 폐기", description = "발급된 액세스 토큰을 폐기합니다.")
    public Mono<ResponseEntity<Void>> revokeToken() {
        return kiwoomAuthAdapter.revokeToken()
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(e -> {
                    log.error("Error revoking token: {}", e.getMessage(), e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to revoke token: " + e.getMessage()));
                });
    }
}
