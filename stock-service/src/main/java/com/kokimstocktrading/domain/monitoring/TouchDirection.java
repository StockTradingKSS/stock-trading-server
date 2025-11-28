package com.kokimstocktrading.domain.monitoring;

import lombok.Getter;

/**
 * 이평선 터치 방향
 */
@Getter
public enum TouchDirection {

  /**
   * 위에서 아래로 터치 (하향 돌파) 주가가 이평선 위에서 아래로 내려가며 터치
   */
  FROM_ABOVE("위→아래", "하향돌파"),

  /**
   * 아래에서 위로 터치 (상향 돌파) 주가가 이평선 아래에서 위로 올라가며 터치
   */
  FROM_BELOW("아래→위", "상향돌파");

  private final String displayName;
  private final String description;

  TouchDirection(String displayName, String description) {
    this.displayName = displayName;
    this.description = description;
  }

  /**
   * 현재가와 이평선 가격으로 터치 방향 판단
   */
  public static TouchDirection determineDirection(long currentPrice, long calculatedPrice) {
    if (isAbove(currentPrice, calculatedPrice)) {
      return FROM_ABOVE;
    } else {
      return FROM_BELOW;
    }
  }

  /**
   * 현재 가격이 이평선 대비 어느 위치에 있는지
   */
  public static boolean isAbove(long currentPrice, long calculatedPrice) {
    return currentPrice > calculatedPrice;
  }
}
