package com.kokimstocktrading.adapter.out.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 카카오톡 메시지 템플릿
 */
@Getter
@Builder
public class KakaoMessageTemplate {

  @JsonProperty("object_type")
  private String objectType;  // "text"

  private String text;

  @JsonProperty("link")
  private Link link;

  @JsonProperty("button_title")
  private String buttonTitle;

  @Getter
  @Builder
  public static class Link {

    @JsonProperty("web_url")
    private String webUrl;

    @JsonProperty("mobile_web_url")
    private String mobileWebUrl;
  }

  /**
   * 텍스트 메시지 생성
   */
  public static KakaoMessageTemplate createTextMessage(String text) {
    return KakaoMessageTemplate.builder()
        .objectType("text")
        .text(text)
        .link(Link.builder().build())
        .build();
  }
}
