package com.kokimstocktrading.adapter.out.external.kakao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kokimstocktrading.adapter.out.external.kakao.dto.KakaoMessageTemplate;
import com.kokimstocktrading.application.notification.port.out.SendNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 카카오톡 나에게 보내기 어댑터
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KakaoMessageAdapter implements SendNotificationPort {

    @Qualifier("kakaoWebClient")
    private final WebClient kakaoWebClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SEND_TO_ME_URI = "/v2/api/talk/memo/default/send";

    @Override
    public Mono<Void> sendKakaoMessage(String message) {
        log.info("카카오톡 메시지 전송 시작: {}", message);

        try {
            // 메시지 템플릿 생성
            KakaoMessageTemplate template = KakaoMessageTemplate.createTextMessage(message);
            String templateObject = objectMapper.writeValueAsString(template);

            // 요청 파라미터 구성
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("template_object", templateObject);

            return kakaoWebClient.post()
                    .uri(SEND_TO_ME_URI)
                    .body(BodyInserters.fromFormData(params))
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(response -> log.info("카카오톡 메시지 전송 성공: {}", response))
                    .doOnError(error -> log.error("카카오톡 메시지 전송 실패", error))
                    .then();

        } catch (JsonProcessingException e) {
            log.error("카카오톡 메시지 템플릿 생성 실패", e);
            return Mono.error(e);
        }
    }
}
