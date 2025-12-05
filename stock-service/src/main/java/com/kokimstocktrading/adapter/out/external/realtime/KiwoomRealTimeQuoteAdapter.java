package com.kokimstocktrading.adapter.out.external.realtime;

import com.common.ExternalSystemAdapter;
import com.kokimstocktrading.adapter.out.external.config.kiwoom.KiwoomWebSocketClient;
import com.kokimstocktrading.adapter.out.external.config.kiwoom.auth.KiwoomAuthAdapter;
import com.kokimstocktrading.application.realtime.out.SubscribeRealTimeQuotePort;
import com.kokimstocktrading.domain.realtime.RealTimeQuote;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@ExternalSystemAdapter
@Slf4j
public class KiwoomRealTimeQuoteAdapter implements SubscribeRealTimeQuotePort, DisposableBean {

  private static final String REAL_TIME_MESSAGE_TYPE = "REAL";
  private static final String STOCK_EXECUTION_TYPE = "0B";
  private static final int QUOTE_CACHE_SIZE = 100;

  private final String webSocketUrl;
  private final KiwoomAuthAdapter kiwoomAuthAdapter;
  private final Sinks.Many<RealTimeQuote> quoteSink;
  private final Flux<RealTimeQuote> quoteFlux;

  // 종목코드 -> 그룹번호 매핑
  private final Map<String, String> stockGroupMap = new ConcurrentHashMap<>();
  // 그룹번호 -> 종목코드 리스트 매핑
  private final Map<String, List<String>> groupStocksMap = new ConcurrentHashMap<>();

  private KiwoomWebSocketClient webSocketClient;
  private boolean isInitialized = false;

  public KiwoomRealTimeQuoteAdapter(
      @Value("${kiwoom.websocket.url:wss://api.kiwoom.com:10000/api/dostk/websocket}") String webSocketUrl,
      KiwoomAuthAdapter kiwoomAuthAdapter) {
    this.webSocketUrl = webSocketUrl;
    this.kiwoomAuthAdapter = kiwoomAuthAdapter;
    this.quoteSink = Sinks.many().multicast().onBackpressureBuffer();
    this.quoteFlux = quoteSink.asFlux().cache(QUOTE_CACHE_SIZE);
  }

  private synchronized void initializeWebSocketIfNeeded() {
    if (isWebSocketConnected()) {
      return;
    }

    try {
      String token = acquireAuthToken();
      closeExistingConnection();
      createAndConnectWebSocket(token);

      log.info("Kiwoom WebSocket 연결 초기화 완료");
    } catch (Exception e) {
      log.error("WebSocket 초기화 중 오류 발생", e);
      isInitialized = false;
    }
  }

  private boolean isWebSocketConnected() {
    return isInitialized && webSocketClient != null && webSocketClient.isConnected();
  }

  private String acquireAuthToken() {
    String token = kiwoomAuthAdapter.getValidToken().block();
    if (token == null) {
      throw new IllegalStateException("실시간 시세 연결을 위한 토큰을 획득할 수 없습니다.");
    }
    return token;
  }

  private void closeExistingConnection() {
    if (webSocketClient != null) {
      try {
        webSocketClient.close();
      } catch (Exception e) {
        log.warn("이전 WebSocket 연결 종료 중 오류 발생", e);
      }
    }
  }

  private void createAndConnectWebSocket(String token) throws Exception {
    URI uri = new URI(webSocketUrl);
    webSocketClient = new KiwoomWebSocketClient(uri, token);

    Sinks.Many<Map<String, Object>> messageSink = Sinks.many().unicast().onBackpressureBuffer();
    messageSink.asFlux().subscribe(this::processWebSocketMessage);
    webSocketClient.setMessageSink(messageSink);

    webSocketClient.connectBlocking();
    isInitialized = true;
  }

  @SuppressWarnings("unchecked")
  private void processWebSocketMessage(Map<String, Object> message) {
    try {
      if (!isRealTimeMessage(message)) {
        return;
      }

      List<Map<String, Object>> dataList = (List<Map<String, Object>>) message.get("data");
      if (dataList == null || dataList.isEmpty()) {
        return;
      }

      dataList.forEach(this::processQuoteData);
    } catch (Exception e) {
      log.error("실시간 시세 처리 중 오류 발생", e);
    }
  }

  private boolean isRealTimeMessage(Map<String, Object> message) {
    return REAL_TIME_MESSAGE_TYPE.equals(message.get("trnm"));
  }

  @SuppressWarnings("unchecked")
  private void processQuoteData(Map<String, Object> data) {
    String type = (String) data.get("type");
    if (!STOCK_EXECUTION_TYPE.equals(type)) {
      return;
    }

    String name = (String) data.get("name");
    String item = (String) data.get("item");
    Map<String, Object> values = (Map<String, Object>) data.get("values");

    if (values == null) {
      return;
    }

    RealTimeQuote quote = buildRealTimeQuote(type, name, item, values);
    emitQuote(quote);
  }

  private RealTimeQuote buildRealTimeQuote(String type, String name, String item,
      Map<String, Object> values) {
    String tradeTimeStr = (String) values.get("20");
    LocalDateTime tradeTime = parseTradeTime(tradeTimeStr);

    return RealTimeQuote.builder()
        .type(type)
        .name(name)
        .item(item)
        .currentPrice((String) values.get("10"))
        .priceChange((String) values.get("11"))
        .changeRate((String) values.get("12"))
        .askPrice((String) values.get("27"))
        .bidPrice((String) values.get("28"))
        .tradingVolume((String) values.get("15"))
        .accumulatedVolume((String) values.get("13"))
        .accumulatedAmount((String) values.get("14"))
        .openPrice((String) values.get("16"))
        .highPrice((String) values.get("17"))
        .lowPrice((String) values.get("18"))
        .tradeTime(tradeTime)
        .build();
  }

  private void emitQuote(RealTimeQuote quote) {
    quoteSink.tryEmitNext(quote);
    log.debug("실시간 시세 수신: 종목={}, 현재가={}, 등락율={}, 거래량={}",
        quote.item(), quote.currentPrice(), quote.changeRate(), quote.tradingVolume());
  }

  @Override
  public Flux<RealTimeQuote> subscribeStockQuote(List<String> stockCodes) {
    if (stockCodes == null || stockCodes.isEmpty()) {
      return Flux.empty();
    }

    initializeWebSocketIfNeeded();

    if (!isInitialized) {
      log.error("WebSocket 초기화 실패로 인해 실시간 시세를 구독할 수 없습니다.");
      return Flux.empty();
    }

    List<String> unsubscribedStocks = findUnsubscribedStocks(stockCodes);
    if (!unsubscribedStocks.isEmpty()) {
      subscribeNewStocks(unsubscribedStocks);
    }

    return quoteFlux.filter(quote -> stockCodes.contains(quote.item()));
  }

  private List<String> findUnsubscribedStocks(List<String> stockCodes) {
    return stockCodes.stream()
        .filter(stockCode -> !stockGroupMap.containsKey(stockCode))
        .toList();
  }

  private void subscribeNewStocks(List<String> stockCodes) {
    String groupNo = webSocketClient.subscribeStocks(stockCodes);

    if (groupNo != null) {
      stockCodes.forEach(stockCode -> stockGroupMap.put(stockCode, groupNo));
      groupStocksMap.put(groupNo, stockCodes);
      log.info("종목 실시간 시세 구독 완료: {} (그룹: {})", stockCodes, groupNo);
    } else {
      log.error("종목 실시간 시세 구독 실패: {}", stockCodes);
    }
  }

  @Override
  public boolean unsubscribeStockQuote(List<String> stockCodes) {
    if (stockCodes == null || stockCodes.isEmpty() || !isInitialized) {
      return false;
    }

    List<String> subscribedStocks = findSubscribedStocks(stockCodes);
    if (subscribedStocks.isEmpty()) {
      log.warn("구독되지 않은 종목에 대한 구독 해지 요청: {}", stockCodes);
      return false;
    }

    return unsubscribeStocksByGroup(subscribedStocks);
  }

  private List<String> findSubscribedStocks(List<String> stockCodes) {
    return stockCodes.stream()
        .filter(stockGroupMap::containsKey)
        .toList();
  }

  private boolean unsubscribeStocksByGroup(List<String> stockCodes) {
    Map<String, List<String>> groupedStocks = groupStocksByGroupNumber(stockCodes);
    boolean allSuccess = true;

    for (Map.Entry<String, List<String>> entry : groupedStocks.entrySet()) {
      String groupNo = entry.getKey();
      List<String> stocks = entry.getValue();

      if (shouldUnsubscribeGroup(groupNo, stocks)) {
        allSuccess &= unsubscribeGroup(groupNo, stocks);
      } else {
        removeStocksFromGroup(groupNo, stocks);
      }
    }

    return allSuccess;
  }

  private Map<String, List<String>> groupStocksByGroupNumber(List<String> stockCodes) {
    return stockCodes.stream()
        .collect(java.util.stream.Collectors.groupingBy(stockGroupMap::get));
  }

  private boolean shouldUnsubscribeGroup(String groupNo, List<String> stocksToRemove) {
    List<String> allStocksInGroup = groupStocksMap.get(groupNo);
    return allStocksInGroup != null && allStocksInGroup.size() == stocksToRemove.size();
  }

  private boolean unsubscribeGroup(String groupNo, List<String> stocks) {
    boolean result = webSocketClient.unsubscribeStocks(groupNo);

    if (result) {
      stocks.forEach(stockGroupMap::remove);
      groupStocksMap.remove(groupNo);
      log.info("종목 실시간 시세 구독 해지 완료: {} (그룹: {})", stocks, groupNo);
    } else {
      log.error("종목 실시간 시세 구독 해지 실패: {} (그룹: {})", stocks, groupNo);
    }

    return result;
  }

  private void removeStocksFromGroup(String groupNo, List<String> stocks) {
    stocks.forEach(stockGroupMap::remove);
    List<String> remainingStocks = groupStocksMap.get(groupNo);
    if (remainingStocks != null) {
      remainingStocks.removeAll(stocks);
    }
    log.info("그룹에서 일부 종목 제거: {} (그룹: {})", stocks, groupNo);
  }

  @Override
  public boolean unsubscribeAllStockQuotes() {
    if (!isInitialized) {
      return false;
    }

    boolean result = webSocketClient.unsubscribeAllGroups();

    if (result) {
      clearAllSubscriptions();
      log.info("모든 종목 실시간 시세 구독 해지 완료");
    } else {
      log.error("모든 종목 실시간 시세 구독 해지 실패");
    }

    return result;
  }

  private void clearAllSubscriptions() {
    stockGroupMap.clear();
    groupStocksMap.clear();
  }

  @Override
  public void destroy() {
    try {
      if (webSocketClient != null) {
        unsubscribeAllStockQuotes();
        webSocketClient.close();
        log.info("WebSocket 연결 종료 완료");
      }
    } catch (Exception e) {
      log.error("WebSocket 연결 종료 중 오류 발생", e);
    }
  }

  /**
   * 체결시간 문자열(HHMMSS)을 LocalDateTime으로 변환
   *
   * @param tradeTimeStr 체결시간 (예: "161402")
   * @return LocalDateTime (오늘 날짜 + 시분초)
   */
  private LocalDateTime parseTradeTime(String tradeTimeStr) {
    if (tradeTimeStr == null || tradeTimeStr.length() != 6) {
      return LocalDateTime.now(); // 기본값
    }

    try {
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
      LocalTime time = LocalTime.parse(tradeTimeStr, timeFormatter);
      return LocalDateTime.of(LocalDate.now(), time);
    } catch (Exception e) {
      log.warn("체결시간 파싱 실패: {}", tradeTimeStr, e);
      return LocalDateTime.now();
    }
  }
}
