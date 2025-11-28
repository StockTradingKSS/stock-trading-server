package com.kokimstocktrading.adapter.out.external.slack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Slack 메시지 요청 DTO
 */
@Getter
@Builder
public class SlackMessageRequest {

  @JsonProperty("channel")
  private String channel;

  @JsonProperty("text")
  private String text;

  @JsonProperty("blocks")
  private Object[] blocks;

  /**
   * 간단한 텍스트 메시지 생성
   */
  public static SlackMessageRequest createSimpleMessage(String channel, String message) {
    return SlackMessageRequest.builder()
        .channel(channel)
        .text(message)
        .build();
  }

  /**
   * 포맷팅된 블록 메시지 생성 (마크다운 지원)
   */
  public static SlackMessageRequest createBlockMessage(String channel, String message) {
    Object[] blocks = new Object[]{
        new Block("section", new TextField("mrkdwn", message))
    };

    return SlackMessageRequest.builder()
        .channel(channel)
        .text(message)  // fallback용
        .blocks(blocks)
        .build();
  }

  @Getter
  @Builder
  private static class Block {

    @JsonProperty("type")
    private String type;

    @JsonProperty("text")
    private TextField text;

    public Block(String type, TextField text) {
      this.type = type;
      this.text = text;
    }
  }

  @Getter
  @Builder
  private static class TextField {

    @JsonProperty("type")
    private String type;

    @JsonProperty("text")
    private String text;

    public TextField(String type, String text) {
      this.type = type;
      this.text = text;
    }
  }
}
