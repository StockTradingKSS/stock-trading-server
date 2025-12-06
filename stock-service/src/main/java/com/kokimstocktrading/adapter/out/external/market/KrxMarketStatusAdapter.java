package com.kokimstocktrading.adapter.out.external.market;

import com.kokimstocktrading.application.market.port.out.LoadMarketStatusPort;
import com.kokimstocktrading.domain.market.MarketStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * KRX 시장 상태 조회 어댑터
 */
@Component
@Slf4j
public class KrxMarketStatusAdapter implements LoadMarketStatusPort {

  private final WebClient webClient;

  public KrxMarketStatusAdapter(@Qualifier("krxWebClient") WebClient krxWebClient) {
    // API 호출용 WebClient
    this.webClient = krxWebClient;
  }

  @Override
  public Mono<MarketStatus> loadMarketStatus(LocalDate date) {
    log.debug("KRX 시장 상태 조회 요청: {}", date);

    // 주말 체크 (토/일은 무조건 휴장)
    if (isWeekend(date)) {
      return Mono.just(MarketStatus.closed(date, "주말"));
    }

    return requestKrxCalendar(date)
        .map(html -> parseMarketStatus(html, date))
        .switchIfEmpty(Mono.fromSupplier(() -> {
          log.warn("KRX API가 빈 응답을 반환했습니다. 날짜: {}. 기본값(개장)을 사용합니다.", date);
          return MarketStatus.open(date);
        }))
        .doOnSuccess(status -> {
          if (status != null) {
            log.debug("시장 상태 조회 완료: {} - {}", date, status.isOpen() ? "개장" : "휴장");
          }
        })
        .doOnError(error -> log.error("시장 상태 조회 실패: {}", date, error));
  }

  @Override
  public Mono<MarketStatus> loadTodayMarketStatus() {
    return loadMarketStatus(LocalDate.now());
  }

  /**
   * KRX 캘린더 API 요청
   */
  private Mono<String> requestKrxCalendar(LocalDate date) {
    MultiValueMap<String, String> formData = createFormData(date);

    return webClient.post()
        .uri("/common/stockschedule.do")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        .body(BodyInserters.fromFormData(formData))
        .retrieve()
        .bodyToMono(String.class)
        .switchIfEmpty(Mono.defer(() -> {
          log.warn("KRX API 응답이 비어있습니다. 날짜: {}", date);
          return Mono.empty();
        }))
        .onErrorMap(error -> new RuntimeException("KRX API 요청 실패", error));
  }

  /**
   * Form Data 생성
   */
  private MultiValueMap<String, String> createFormData(LocalDate date) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

    formData.add("method", "searchTraidingCalendar");
    formData.add("forward", "searchtraidingcalendar");
    formData.add("searchCodeType", "");
    formData.add("repIsuSrtCd", "");
    formData.add("menuIndex", "");
    formData.add("selMonth", String.format("%02d", date.getMonthValue()));
    formData.add("selDay", "");
    formData.add("enterFlag", "");
    formData.add("searchMenu", "13");
    formData.add("nowYear", String.valueOf(date.getYear()));
    formData.add("nowMonth", String.valueOf(date.getMonthValue()));
    formData.add("searchCorpName", "회사명/코드");
    formData.add("selYear", String.valueOf(date.getYear()));
    formData.add("showMonth", String.valueOf(date.getMonthValue()));
    formData.add("periodType", "M");
    formData.add("selDate", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    formData.add("ldMktTpCd", "");
    formData.add("submitFlagYn", "N");

    return formData;
  }

  /**
   * HTML에서 시장 상태 파싱
   */
  private MarketStatus parseMarketStatus(String html, LocalDate targetDate) {
    try {
      Document document = Jsoup.parse(html);
      Elements tbody = document.select("#calBig > table > tbody");

      if (tbody.isEmpty()) {
        log.warn("캘린더 데이터를 찾을 수 없습니다");
        return MarketStatus.open(targetDate); // 기본값: 개장
      }

      // 모든 td 요소 조회
      Elements allCells = tbody.select("td");

      for (Element cell : allCells) {
        // 날짜 텍스트 추출
        String dayText = extractDayFromCell(cell);
        if (dayText == null || dayText.trim().isEmpty()) {
          continue;
        }

        try {
          int day = Integer.parseInt(dayText.trim());
          if (day == targetDate.getDayOfMonth()) {
            return analyzeCell(cell, targetDate);
          }
        } catch (NumberFormatException e) {
          // 숫자가 아닌 경우 건너뛰기
          continue;
        }
      }

      // 해당 날짜를 찾지 못한 경우 기본값: 개장
      return MarketStatus.open(targetDate);

    } catch (Exception e) {
      log.error("HTML 파싱 실패", e);
      return MarketStatus.open(targetDate); // 오류 시 기본값: 개장
    }
  }

  /**
   * 셀에서 날짜 추출
   */
  private String extractDayFromCell(Element cell) {
    String cellText = cell.ownText(); // 직접 텍스트만 추출

    // 클래스가 있는 셀의 경우 숫자 부분만 추출
    if (cellText.matches("\\d+.*")) {
      return cellText.replaceAll("[^\\d]", "");
    }

    return cellText;
  }

  /**
   * 셀 분석하여 시장 상태 결정
   */
  private MarketStatus analyzeCell(Element cell, LocalDate date) {
    // 주말 체크 (sun, sat 클래스)
    if (cell.hasClass("sun") || cell.hasClass("sat")) {
      return MarketStatus.closed(date, "주말");
    }

    // li 요소 내용 확인
    Elements liElements = cell.select("li");
    for (Element li : liElements) {
      String liText = li.text().trim();
      if (!liText.isEmpty()) {
        // 공휴일 정보가 있으면 휴장
        return MarketStatus.closed(date, liText);
      }
    }

    // 조건에 해당하지 않으면 개장
    return MarketStatus.open(date);
  }

  /**
   * 주말 여부 확인
   */
  private boolean isWeekend(LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
  }
}
