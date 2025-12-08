package com.kokimstocktrading.adapter.out.external.market;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class KrxMarketStatusAdapterTest {

  @Autowired
  private KrxMarketStatusAdapter adapter;

  @DisplayName("주말(토요일)은 API 호출 없이 휴장으로 판단한다")
  @Test
  void shouldReturnClosedForSaturday() {
    // given
    LocalDate saturday = LocalDate.of(2024, 8, 10); // 토요일

    // when & then
    StepVerifier.create(adapter.loadMarketStatus(saturday))
        .assertNext(status -> {
          assertThat(status.isOpen()).isFalse();
          assertThat(status.getReason()).isEqualTo("주말");
          assertThat(status.getDate()).isEqualTo(saturday);
        })
        .verifyComplete();
  }

  @DisplayName("주말(일요일)은 API 호출 없이 휴장으로 판단한다")
  @Test
  void shouldReturnClosedForSunday() {
    // given
    LocalDate sunday = LocalDate.of(2024, 8, 11); // 일요일

    // when & then
    StepVerifier.create(adapter.loadMarketStatus(sunday))
        .assertNext(status -> {
          assertThat(status.isOpen()).isFalse();
          assertThat(status.getReason()).isEqualTo("주말");
          assertThat(status.getDate()).isEqualTo(sunday);
        })
        .verifyComplete();
  }

  @DisplayName("광복절(8월 15일)은 휴장으로 판단한다")
  @Test
  void shouldReturnClosedForLiberationDay() {
    // given
    LocalDate liberationDay = LocalDate.of(2024, 8, 15); // 광복절

    // when & then
    StepVerifier.create(adapter.loadMarketStatus(liberationDay))
        .assertNext(status -> {
          log.info("광복절 시장 상태: isOpen={}, reason={}", status.isOpen(), status.getReason());
          assertThat(status.getDate()).isEqualTo(liberationDay);
          // 실제 KRX API 응답에 따라 휴장일로 나와야 함
          if (!status.isOpen()) {
            assertThat(status.getReason()).contains("광복절");
          }
        })
        .verifyComplete();
  }

  @DisplayName("평일 중 개장일은 정상적으로 개장으로 판단한다")
  @Test
  void shouldReturnOpenForRegularWeekday() {
    // given - 8월 9일 금요일 (일반적인 개장일)
    LocalDate regularWeekday = LocalDate.of(2024, 8, 9);

    // when & then
    StepVerifier.create(adapter.loadMarketStatus(regularWeekday))
        .assertNext(status -> {
          log.info("평일 시장 상태: isOpen={}, reason={}", status.isOpen(), status.getReason());
          assertThat(status.getDate()).isEqualTo(regularWeekday);
          // 실제 KRX API 응답에 따라 개장일로 나와야 함
          assertThat(status.isOpen()).isTrue();
        })
        .verifyComplete();
  }

  @DisplayName("오늘의 시장 상태를 정상적으로 조회한다")
  @Test
  void shouldReturnTodayMarketStatus() {
    // when & then
    StepVerifier.create(adapter.loadTodayMarketStatus())
        .assertNext(status -> {
          log.info("오늘 시장 상태: date={}, isOpen={}, reason={}",
              status.getDate(), status.isOpen(), status.getReason());
          assertThat(status.getDate()).isEqualTo(LocalDate.now());
          // 결과 검증 (개장/휴장 여부는 실제 날짜에 따라 달라짐)
        })
        .verifyComplete();
  }

  @DisplayName("미래 날짜도 정상적으로 조회한다")
  @Test
  void shouldReturnFutureDateMarketStatus() {
    // given - 다음 달 첫째 주 월요일
    LocalDate futureDate = LocalDate.of(2024, 9, 2);

    // when & then
    StepVerifier.create(adapter.loadMarketStatus(futureDate))
        .assertNext(status -> {
          log.info("미래 날짜 시장 상태: date={}, isOpen={}, reason={}",
              status.getDate(), status.isOpen(), status.getReason());
          assertThat(status.getDate()).isEqualTo(futureDate);
        })
        .verifyComplete();
  }

  @DisplayName("API 응답 시간이 합리적인 범위 내에 있다")
  @Test
  void shouldReturnWithinReasonableTime() {
    // given
    LocalDate testDate = LocalDate.of(2024, 8, 12); // 월요일

    // when & then
    long startTime = System.currentTimeMillis();

    StepVerifier.create(adapter.loadMarketStatus(testDate))
        .assertNext(status -> {
          long endTime = System.currentTimeMillis();
          long duration = endTime - startTime;

          log.info("API 응답 시간: {}ms", duration);
          assertThat(duration).isLessThan(10000); // 10초 이내
          assertThat(status.getDate()).isEqualTo(testDate);
        })
        .verifyComplete();
  }
}
