package com.kokimstocktrading.adapter.out.external.slack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Slack 메시지 응답 DTO
 */
@Getter
@NoArgsConstructor
public class SlackMessageResponse {

  @JsonProperty("ok")
  private Boolean ok;

  @JsonProperty("channel")
  private String channel;

  @JsonProperty("ts")
  private String ts;

  @JsonProperty("error")
  private String error;
}
