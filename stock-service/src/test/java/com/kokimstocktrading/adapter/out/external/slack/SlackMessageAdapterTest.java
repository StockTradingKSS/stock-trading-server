package com.kokimstocktrading.adapter.out.external.slack;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

/**
 * Slack 메시지 전송 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class SlackMessageAdapterTest {

  @Autowired
  private SlackMessageAdapter slackMessageAdapter;

  @DisplayName("슬랙 메시지 전송 테스트")
  @Test
  void testSendMessage() {
    String testMessage = String.format(
        "[테스트 알림]\n테스트 시간: %s\n상태: 정상 작동",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    );

    StepVerifier.create(slackMessageAdapter.sendMessage(testMessage))
        .expectComplete()
        .verify();

    log.info("슬랙 메시지 전송 테스트 완료 - 슬랙 채널을 확인하세요!");
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

    StepVerifier.create(slackMessageAdapter.sendMessage(message))
        .expectComplete()
        .verify();

    log.info("거래 조건 알림 테스트 완료 - 슬랙 채널을 확인하세요!");
  }

  @DisplayName("마크다운 형식 메시지 테스트")
  @Test
  void testMarkdownMessage() {
    String message = "*[거래 조건 알림]*\n"
        + "• 종목: `005930` (삼성전자)\n"
        + "• 조건: 20일 이평선 터치\n"
        + "• 현재가: *75,000원*\n"
        + "• 시간: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

    StepVerifier.create(slackMessageAdapter.sendMessage(message))
        .expectComplete()
        .verify();

    log.info("마크다운 메시지 테스트 완료 - 슬랙 채널을 확인하세요!");
  }

  @DisplayName("연속 메시지 전송 테스트")
  @Test
  void testMultipleMessages() {
    for (int i = 1; i <= 3; i++) {
      String message = String.format("*[테스트 %d]* 연속 메시지 전송 테스트", i);

      StepVerifier.create(slackMessageAdapter.sendMessage(message))
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

    log.info("연속 메시지 전송 테스트 완료 - 슬랙 채널에서 3개 메시지를 확인하세요!");
  }

  @DisplayName("에러 처리 테스트 - 잘못된 채널")
  @Test
  void testInvalidChannel() {
    String message = "테스트 메시지";
    String invalidChannel = "nonexistent-channel-12345";

    StepVerifier.create(slackMessageAdapter.sendSlackMessage(invalidChannel, message))
        .expectError()
        .verify();

    log.info("에러 처리 테스트 완료 - 잘못된 채널에 대한 에러 처리 확인");
  }

  @DisplayName("긴 메시지 전송 테스트")
  @Test
  void testLongMessage() {
    StringBuilder longMessage = new StringBuilder("*[상세 거래 알림]*\n\n");
    longMessage.append("종목: 삼성전자 (005930)\n");
    longMessage.append("조건: 20일 이평선 상향 돌파\n\n");
    longMessage.append("*상세 정보:*\n");
    longMessage.append("• 현재가: 75,000원\n");
    longMessage.append("• 전일 대비: +2.5%\n");
    longMessage.append("• 거래량: 1,234,567주\n");
    longMessage.append("• 20일 이평선: 73,500원\n");
    longMessage.append("• 60일 이평선: 72,000원\n");
    longMessage.append("• RSI: 65.3\n");
    longMessage.append("\n시간: ").append(
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

    StepVerifier.create(slackMessageAdapter.sendMessage(longMessage.toString()))
        .expectComplete()
        .verify();

    log.info("긴 메시지 전송 테스트 완료 - 슬랙 채널을 확인하세요!");
  }
}
