package com.kokimstocktrading.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 카카오 API 웹클라이언트 설정
 */
@Configuration
@Slf4j
public class KakaoWebClientConfig {

    @Value("${kakao.api.base-url}")
    private String kakaoApiBaseUrl;

    @Bean(name = "kakaoWebClient")
    public WebClient kakaoWebClient() {
        log.info("카카오 웹클라이언트 초기화: baseUrl={}", kakaoApiBaseUrl);

        return WebClient.builder()
                .baseUrl(kakaoApiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }
}
