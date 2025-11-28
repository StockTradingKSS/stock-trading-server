package com.kokimstocktrading.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Slack API 웹클라이언트 설정
 */
@Configuration
@Slf4j
public class SlackWebClientConfig {

  @Value("${slack.api.base-url}")
  private String slackApiBaseUrl;

  @Value("${slack.api.bot-token}")
  private String slackBotToken;

  @Bean(name = "slackWebClient")
  public WebClient slackWebClient() {
    log.info("슬랙 웹클라이언트 초기화: baseUrl={}", slackApiBaseUrl);

    return WebClient.builder()
        .baseUrl(slackApiBaseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + slackBotToken)
        .build();
  }
}
