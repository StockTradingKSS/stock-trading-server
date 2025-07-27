package com.kokimstocktrading.application.realtime.out;

import com.kokimstocktrading.domain.realtime.RealTimeQuote;
import reactor.core.publisher.Flux;

import java.util.List;

public interface SubscribeRealTimeQuotePort {
    /**
     * 실시간 주식 시세 구독
     *
     * @param stockCodes 실시간 시세를 받을 종목 코드 목록
     * @return 실시간 시세 Flux 스트림
     */
    Flux<RealTimeQuote> subscribeStockQuote(List<String> stockCodes);

    /**
     * 실시간 시세 구독 해지
     *
     * @param stockCodes 구독 해지할 종목 코드 목록
     * @return 구독 해지 성공 여부
     */
    boolean unsubscribeStockQuote(List<String> stockCodes);

    /**
     * 모든 실시간 시세 구독 해지
     *
     * @return 구독 해지 성공 여부
     */
    boolean unsubscribeAllStockQuotes();
}
