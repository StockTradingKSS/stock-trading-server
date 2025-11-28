package com.kokimstocktrading.adapter.out.external.kakao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

/**
 * 카카오톡 메시지 전송 통합 테스트
 *
 * @deprecated Slack으로 알림 시스템이 전환되었습니다. {@link
 *     com.kokimstocktrading.adapter.out.external.slack.SlackMessageAdapterTest}를 사용하세요.
 */
@Deprecated
@Disabled("Slack으로 전환되어 비활성화됨")
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class KakaoMessageAdapterTest {

    @Autowired
    private KakaoMessageAdapter kakaoMessageAdapter;

    @Autowired
    private KakaoTokenService kakaoTokenService;

    @DisplayName("Access Token 갱신 테스트")
    @Test
    void testTokenRefresh() {
        StepVerifier.create(kakaoTokenService.getValidAccessToken())
                .assertNext(accessToken -> {
                    log.info("Access Token 발급 성공: {}", accessToken.substring(0, 20) + "...");
                })
                .verifyComplete();
    }

    @DisplayName("카카오톡 나에게 보내기 테스트")
    @Test
    void testSendMessage() {
        String testMessage = String.format(
                "[테스트 알림]\n테스트 시간: %s\n상태: 정상 작동",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        StepVerifier.create(kakaoMessageAdapter.sendMessage(testMessage))
                .expectComplete()
                .verify();

        log.info("카카오톡 메시지 전송 테스트 완료 - 카카오톡을 확인하세요!");
    }

    @DisplayName("거래 조건 발동 알림 형식 테스트")
    @Test
    void testTradingConditionAlert() {
        String stockCode = "005930";
        String description = "20일 이평선 상향 돌파";

        String message = String.format(
                "[이평선 조건 발동]\n종목: %s (삼성전자)\n설명: %s\n시간: %s",
                stockCode,
                description,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        );

        StepVerifier.create(kakaoMessageAdapter.sendMessage(message))
                .expectComplete()
                .verify();

        log.info("거래 조건 알림 테스트 완료 - 카카오톡을 확인하세요!");
    }

    @DisplayName("연속 메시지 전송 테스트")
    @Test
    void testMultipleMessages() {
        for (int i = 1; i <= 3; i++) {
            String message = String.format("[테스트 %d] 연속 메시지 전송 테스트", i);

            StepVerifier.create(kakaoMessageAdapter.sendMessage(message))
                    .expectComplete()
                    .verify();

            log.info("메시지 {} 전송 완료", i);

            // API 호출 제한을 위한 대기
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("연속 메시지 전송 테스트 완료 - 카카오톡에서 3개 메시지를 확인하세요!");
    }
}
