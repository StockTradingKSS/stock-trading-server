package com.kokimstocktrading.adapter.out.external.slack;

import com.kokimstocktrading.adapter.out.external.slack.dto.SlackMessageRequest;
import com.kokimstocktrading.adapter.out.external.slack.dto.SlackMessageResponse;
import com.kokimstocktrading.application.notification.port.out.SendNotificationPort;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Slack 메시지 전송 어댑터
 */
@Component
@Primary  // KakaoMessageAdapter 대신 기본으로 사용
@Slf4j
@RequiredArgsConstructor
public class SlackMessageAdapter implements SendNotificationPort {

  private static final String POST_MESSAGE_URI = "/chat.postMessage";
  // 환경별 채널 이름 (Java 코드로 하드코딩)
  private static final String PROD_CHANNEL = "#stock-monitoring-alarm";
  private static final String LOCAL_CHANNEL = "#stock-monitoring-alarm-test";
  @Qualifier("slackWebClient")
  private final WebClient slackWebClient;
  private final Environment environment;
  private String channelName;

  @PostConstruct
  public void init() {
    String[] activeProfiles = environment.getActiveProfiles();
    log.info("활성 프로필: {}", Arrays.toString(activeProfiles));

    // 프로필에 따라 채널 이름 결정
    if (Arrays.asList(activeProfiles).contains("prod")) {
      channelName = PROD_CHANNEL;
    } else {
      // local 또는 기본값
      channelName = LOCAL_CHANNEL;
    }

    log.info("슬랙 채널 설정 완료: {}", channelName);
  }

  @Override
  public Mono<Void> sendMessage(String message) {
    return sendSlackMessage(message);
  }

  /**
   * Slack 메시지 전송 (기본 채널)
   */
  public Mono<Void> sendSlackMessage(String message) {
    return sendSlackMessage(channelName, message);
  }

  /**
   * Slack 메시지 전송 (지정 채널)
   */
  public Mono<Void> sendSlackMessage(String channel, String message) {
    log.info("슬랙 메시지 전송 시작: channel={}, message={}", channel, message);

    // 블록 메시지 생성 (마크다운 지원)
    SlackMessageRequest request = SlackMessageRequest.createBlockMessage(channel, message);

    return slackWebClient.post()
        .uri(POST_MESSAGE_URI)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(SlackMessageResponse.class)
        .flatMap(response -> {
          if (Boolean.TRUE.equals(response.getOk())) {
            log.info("슬랙 메시지 전송 성공: channel={}, ts={}", response.getChannel(),
                response.getTs());
            return Mono.empty();
          } else {
            log.error("슬랙 메시지 전송 실패: error={}", response.getError());
            return Mono.error(
                new RuntimeException("슬랙 메시지 전송 실패: " + response.getError()));
          }
        })
        .doOnError(error -> log.error("슬랙 API 호출 실패: channel={}", channel, error))
        .then();
  }
}
