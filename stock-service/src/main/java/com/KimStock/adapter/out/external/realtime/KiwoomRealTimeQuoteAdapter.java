package com.KimStock.adapter.out.external.realtime;

import com.KimStock.adapter.out.external.kiwoom.KiwoomWebSocketClient;
import com.KimStock.adapter.out.external.kiwoom.auth.KiwoomAuthAdapter;
import com.KimStock.application.port.out.SubscribeRealTimeQuotePort;
import com.KimStock.domain.model.RealTimeQuote;
import com.common.ExternalSystemAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongConsumer;

@ExternalSystemAdapter
@Slf4j
public class KiwoomRealTimeQuoteAdapter implements SubscribeRealTimeQuotePort, DisposableBean {

    private final String webSocketUrl;
    private final KiwoomAuthAdapter kiwoomAuthAdapter;
    private final Sinks.Many<RealTimeQuote> quoteSink;
    private final Flux<RealTimeQuote> quoteFlux;
    // 그룹별 구독 정보 관리
    private final Map<List<String>, String> stockGroupMap = new ConcurrentHashMap<>();
    private KiwoomWebSocketClient webSocketClient;
    // 접속 상태
    private boolean isInitialized = false;

    public KiwoomRealTimeQuoteAdapter(
            @Value("${kiwoom.websocket.url:wss://api.kiwoom.com:10000/api/dostk/websocket}") String webSocketUrl,
            KiwoomAuthAdapter kiwoomAuthAdapter) {
        this.webSocketUrl = webSocketUrl;
        this.kiwoomAuthAdapter = kiwoomAuthAdapter;

        // Non-unicast sink: 여러 구독자가 데이터를 받을 수 있도록 함
        this.quoteSink = Sinks.many().multicast().onBackpressureBuffer();
        this.quoteFlux = quoteSink.asFlux().cache(100);  // 최근 100개 항목 캐싱
    }

    private synchronized void initializeWebSocketIfNeeded() {
        if (isInitialized && webSocketClient != null && webSocketClient.isConnected()) {
            return;
        }

        try {
            // 토큰 획득
            String token = kiwoomAuthAdapter.getValidToken().block();
            if (token == null) {
                log.error("실시간 시세 연결을 위한 토큰을 획득할 수 없습니다.");
                return;
            }

            // 이전 연결 종료
            if (webSocketClient != null) {
                try {
                    webSocketClient.close();
                } catch (Exception e) {
                    log.warn("이전 WebSocket 연결 종료 중 오류 발생", e);
                }
            }

            // 새 WebSocket 클라이언트 생성 및 연결
            URI uri = new URI(webSocketUrl);
            webSocketClient = new KiwoomWebSocketClient(uri, token);

            // 메시지 처리기 설정
            Sinks.Many<Map<String, Object>> messageSink = Sinks.many().unicast().onBackpressureBuffer();

            // 메시지 구독 및 처리
            messageSink.asFlux().subscribe(this::processWebSocketMessage);

            // 메시지 싱크 설정
            webSocketClient.setMessageSink(new FluxSink<Map<String, Object>>() {
                @Override
                public void complete() {
                    messageSink.tryEmitComplete();
                }

                @Override
                public void error(Throwable e) {
                    messageSink.tryEmitError(e);
                }

                @Override
                public FluxSink<Map<String, Object>> next(Map<String, Object> value) {
                    messageSink.tryEmitNext(value);
                    return this;
                }

                @Override
                public Context currentContext() {
                    return Context.empty();
                }

                @Override
                public long requestedFromDownstream() {
                    return 0;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public FluxSink<Map<String, Object>> onRequest(LongConsumer longConsumer) {
                    return this;
                }

                @Override
                public FluxSink<Map<String, Object>> onCancel(Disposable disposable) {
                    return this;
                }

                @Override
                public FluxSink<Map<String, Object>> onDispose(Disposable disposable) {
                    return this;
                }
            });

            // 연결 시작
            webSocketClient.connectBlocking();
            isInitialized = true;

            log.info("Kiwoom WebSocket 연결 초기화 완료");
        } catch (Exception e) {
            log.error("WebSocket 초기화 중 오류 발생", e);
            isInitialized = false;
        }
    }

    private void processWebSocketMessage(Map<String, Object> message) {
        try {
            if (!"REAL".equals(message.get("trnm"))) {
                return;
            }

            List<Map<String, Object>> dataList = (List<Map<String, Object>>) message.get("data");
            if (dataList == null || dataList.isEmpty()) {
                return;
            }

            for (Map<String, Object> data : dataList) {
                String type = (String) data.get("type");
                String name = (String) data.get("name");
                String item = (String) data.get("item");

                if (!"0B".equals(type)) {  // 주식체결만 처리
                    continue;
                }

                Map<String, Object> values = (Map<String, Object>) data.get("values");
                if (values == null) {
                    continue;
                }

                // 주식체결 실시간 데이터 필드 추출
                String currentPrice = (String) values.get("10");   // 현재가
                String priceChange = (String) values.get("11");    // 전일대비
                String changeRate = (String) values.get("12");     // 등락율
                String askPrice = (String) values.get("27");       // 최우선 매도호가
                String bidPrice = (String) values.get("28");       // 최우선 매수호가
                String tradingVolume = (String) values.get("15");  // 거래량
                String accumulatedVolume = (String) values.get("13"); // 누적거래량
                String accumulatedAmount = (String) values.get("14"); // 누적거래대금
                String openPrice = (String) values.get("16");      // 시가
                String highPrice = (String) values.get("17");      // 고가
                String lowPrice = (String) values.get("18");       // 저가
                String tradeTime = (String) values.get("20");      // 체결시간

                RealTimeQuote quote = RealTimeQuote.builder()
                        .type(type)
                        .name(name)
                        .item(item)
                        .currentPrice(currentPrice)
                        .priceChange(priceChange)
                        .changeRate(changeRate)
                        .askPrice(askPrice)
                        .bidPrice(bidPrice)
                        .tradingVolume(tradingVolume)
                        .accumulatedVolume(accumulatedVolume)
                        .accumulatedAmount(accumulatedAmount)
                        .openPrice(openPrice)
                        .highPrice(highPrice)
                        .lowPrice(lowPrice)
                        .tradeTime(tradeTime)
                        .build();

                // Sink에 데이터 전송
                quoteSink.tryEmitNext(quote);

                log.debug("실시간 시세 수신: 종목={}, 현재가={}, 등락율={}, 거래량={}",
                        item, currentPrice, changeRate, tradingVolume);
            }
        } catch (Exception e) {
            log.error("실시간 시세 처리 중 오류 발생", e);
        }
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

        // 이미 구독 중인 종목인지 확인
        if (!stockGroupMap.containsKey(stockCodes)) {
            String groupNo = webSocketClient.subscribeStocks(stockCodes);
            if (groupNo != null) {
                stockGroupMap.put(stockCodes, groupNo);
                log.info("종목 실시간 시세 구독 완료: {}", stockCodes);
            } else {
                log.error("종목 실시간 시세 구독 실패: {}", stockCodes);
                return Flux.empty();
            }
        }

        // 구독한 종목에 대한 시세만 필터링하여 제공
        return quoteFlux.filter(quote -> stockCodes.contains(quote.item()));
    }

    @Override
    public boolean unsubscribeStockQuote(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty() || !isInitialized) {
            return false;
        }

        String groupNo = stockGroupMap.get(stockCodes);
        if (groupNo == null) {
            log.warn("구독되지 않은 종목에 대한 구독 해지 요청: {}", stockCodes);
            return false;
        }

        boolean result = webSocketClient.unsubscribeStocks(groupNo);
        if (result) {
            stockGroupMap.remove(stockCodes);
            log.info("종목 실시간 시세 구독 해지 완료: {}", stockCodes);
        } else {
            log.error("종목 실시간 시세 구독 해지 실패: {}", stockCodes);
        }

        return result;
    }

    @Override
    public boolean unsubscribeAllStockQuotes() {
        if (!isInitialized) {
            return false;
        }

        boolean result = webSocketClient.unsubscribeAllGroups();
        if (result) {
            stockGroupMap.clear();
            log.info("모든 종목 실시간 시세 구독 해지 완료");
        } else {
            log.error("모든 종목 실시간 시세 구독 해지 실패");
        }

        return result;
    }

    @Override
    public void destroy() {
        // Bean 소멸 시 연결 정리
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
}
