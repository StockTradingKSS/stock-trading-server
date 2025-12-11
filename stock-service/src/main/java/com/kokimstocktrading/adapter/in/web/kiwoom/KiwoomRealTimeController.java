package com.kokimstocktrading.adapter.in.web.kiwoom;

import com.common.WebAdapter;
import com.kokimstocktrading.application.realtime.out.SubscribeRealTimeQuotePort;
import com.kokimstocktrading.config.SseConnectionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@WebAdapter
@RestController
@RequestMapping("/api/kiwoom/realtime")
@Tag(name = "Kiwoom RealTime API", description = "키움증권 실시간 시세 API")
@RequiredArgsConstructor
@Slf4j
public class KiwoomRealTimeController {

  private final SubscribeRealTimeQuotePort subscribeRealTimeQuotePort;
  private final SseConnectionManager sseConnectionManager;

  @GetMapping(value = "/quote", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(summary = "실시간 주식 시세 조회", description = "지정한 종목들의 실시간 시세를 Server-Sent Events로 제공합니다.")
  public Flux<ServerSentEvent<RealTimeQuoteResponse>> getStockRealTimeQuote(
      @Parameter(description = "종목코드(여러 개인 경우 콤마로 구분)", example = "005930,035720")
      @RequestParam String stockCodes) {

    List<String> stockCodeList = Arrays.asList(stockCodes.split(","));
    log.info("실시간 시세 구독 요청: {}", stockCodeList);

    // 실시간 시세 구독 및 응답 변환
    return subscribeRealTimeQuotePort.subscribeStockQuote(stockCodeList)
        .map(RealTimeQuoteResponse::from)
        .map(data -> ServerSentEvent.<RealTimeQuoteResponse>builder()
            .id(data.stockCode())
            .event("quote")
            .data(data)
            .build())
        // SSE 연결 유지를 위한 하트비트 추가
        .mergeWith(Flux.interval(Duration.ofSeconds(30))
            .map(i -> ServerSentEvent.<RealTimeQuoteResponse>builder()
                .event("heartbeat")
                .build()))
        // Graceful shutdown 시 SSE 연결 강제 종료
        .takeUntilOther(sseConnectionManager.getShutdownSignal())
        .doOnComplete(() -> log.info("실시간 시세 구독 종료: {}", stockCodeList))
        .doOnCancel(() -> log.info("실시간 시세 구독 취소: {}", stockCodeList));
  }

  @DeleteMapping("/quote")
  @Operation(summary = "실시간 시세 구독 해지", description = "지정한 종목들의 실시간 시세 구독을 해지합니다.")
  public boolean unsubscribeStockQuote(
      @Parameter(description = "종목코드(여러 개인 경우 콤마로 구분)", example = "005930,035720")
      @RequestParam String stockCodes) {

    List<String> stockCodeList = Arrays.asList(stockCodes.split(","));
    log.info("실시간 시세 구독 해지 요청: {}", stockCodeList);

    return subscribeRealTimeQuotePort.unsubscribeStockQuote(stockCodeList);
  }

  @DeleteMapping("/quote/all")
  @Operation(summary = "모든 실시간 시세 구독 해지", description = "모든 종목의 실시간 시세 구독을 해지합니다.")
  public boolean unsubscribeAllStockQuote() {
    log.info("모든 실시간 시세 구독 해지 요청");
    return subscribeRealTimeQuotePort.unsubscribeAllStockQuotes();
  }
}
