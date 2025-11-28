package com.kokimstocktrading.domain.candle;

import lombok.Getter;

public enum CandleInterval {
  MINUTE("분"), // 1분봉
  DAY("일"),      // 일봉
  WEEK("주"),     // 주봉
  MONTH("월"),    // 월봉
  YEAR("년"),     // 년봉

  ;

  @Getter
  private final String displayName;

  CandleInterval(String displayName) {
    this.displayName = displayName;
  }
}
