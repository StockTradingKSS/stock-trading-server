package com.kokimstocktrading.adapter.out.external.market;

import com.common.ExternalSystemAdapter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kokimstocktrading.adapter.out.external.config.kiwoom.ClientErrorHandler;
import com.kokimstocktrading.adapter.out.external.config.kiwoom.auth.KiwoomAuthAdapter;
import com.kokimstocktrading.application.market.port.out.LoadMarketListPort;
import com.kokimstocktrading.domain.market.Market;
import com.kokimstocktrading.domain.market.MarketType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@ExternalSystemAdapter
public class KiwoomLoadMarketListAdapter implements LoadMarketListPort {

  private final WebClient webClient;
  private final KiwoomAuthAdapter kiwoomAuthAdapter;
  private final ClientErrorHandler clientErrorHandler;

  public KiwoomLoadMarketListAdapter(
      @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient,
      KiwoomAuthAdapter kiwoomAuthAdapter, ClientErrorHandler clientErrorHandler) {

    this.webClient = kiwoomWebClient;
    this.kiwoomAuthAdapter = kiwoomAuthAdapter;
    this.clientErrorHandler = clientErrorHandler;
  }

  /**
   * 종목 정보 리스트 조회
   *
   * @param marketType 시장 유형
   * @return 종목 정보 응답
   */
  public Mono<List<Market>> loadMarketListBy(MarketType marketType, String contYn, String nextKey) {
    MarketCodeRequest marketCodeRequest = MarketCodeRequest.of(marketType);
    return kiwoomAuthAdapter.getValidToken()
        .flatMap(token -> loadMarketCodeListApi(token, marketCodeRequest, contYn, nextKey))
        .flatMap(marketCodeList ->
            Mono.just(marketCodeList.stream()
                .map(MarketCodeResponse.MarketCode::mapToMarket)
                .toList())
        )
        .onErrorResume(e -> {
          log.error("[Load Market List Error] : {}", e.getMessage());
          return Mono.error(e);
        });

  }

  private Mono<List<MarketCodeResponse.MarketCode>> loadMarketCodeListApi(String token,
      MarketCodeRequest marketCodeRequest, String contYn, String nextKey) {

    return webClient.post()
        .uri(uriBuilder -> uriBuilder
            .path("/api/dostk/stkinfo")
            .build())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .header("api-id", "ka10101")
        .header("cont-yn", contYn != null ? contYn : "N")    // 연속조회여부
        .header("next-key", nextKey != null ? nextKey : "")  // 연속조회키
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(marketCodeRequest)
        .exchangeToMono(this::handleMarketCodeResponse);
  }

  private Mono<List<MarketCodeResponse.MarketCode>> handleMarketCodeResponse(
      ClientResponse response) {
    if (response.statusCode().is2xxSuccessful()) {
      return response.bodyToMono(MarketCodeResponse.class)
          .flatMap(marketCodeResponse -> Mono.just(marketCodeResponse.marketCodeList()));
    }
    return clientErrorHandler.handleErrorResponse(response, "Market code request failed.");
  }

  /**
   * 업종코드 요청 DTO
   */
  private record MarketCodeRequest(
      String mrkt_tp     // 0:코스피(거래소),1:코스닥,2:KOSPI200,4:KOSPI100,7:KRX100(통합지수)
  ) {

    @Builder
    public MarketCodeRequest {
      mrkt_tp = Objects.requireNonNullElse(mrkt_tp, "0");
    }

    public static MarketCodeRequest of(MarketType marketType) {
      // 0:코스피,10:코스닥,3:ELW,8:ETF,30:K-OTC,50:코넥스,5:신주인수권,4:뮤추얼펀드,6:리츠,9:하이일드
      String mrkt_tp;

      switch (marketType) {
        case KOSPI -> mrkt_tp = "0";
        case KOSDAQ -> mrkt_tp = "1";
        case null, default -> throw new IllegalArgumentException("marketType is null");
      }

      return MarketCodeRequest.builder()
          .mrkt_tp(mrkt_tp)
          .build();
    }
  }

  /**
   * 업종코드 응답 DTO
   */
  private record MarketCodeResponse(
      String return_msg,
      @JsonProperty(value = "list", access = JsonProperty.Access.WRITE_ONLY)
      List<MarketCode> marketCodeList
  ) {

    @Builder
    public MarketCodeResponse {
      marketCodeList = Objects.requireNonNullElse(marketCodeList, new ArrayList<>());
      return_msg = Objects.requireNonNullElse(return_msg, "null 값이 들어왔습니다.");
    }

    /**
     * 업종코드 아이템
     */
    public record MarketCode(
        String marketCode,           // 시장구분코드
        String code,            // 코드
        String name,           // 업종명
        String group          // 그룹
    ) {

      @Builder
      public MarketCode {
        marketCode = Objects.requireNonNullElse(marketCode, "");
        code = Objects.requireNonNullElse(code, "");
        name = Objects.requireNonNullElse(name, "");
        group = Objects.requireNonNullElse(group, "");
      }

      public Market mapToMarket() {
        return Market.builder()
            .marketCode(marketCode)
            .code(code)
            .name(name)
            .group(group)
            .build();
      }
    }
  }
}
