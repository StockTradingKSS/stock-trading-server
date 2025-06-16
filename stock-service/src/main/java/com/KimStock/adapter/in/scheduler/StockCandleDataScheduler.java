package com.KimStock.adapter.in.scheduler;

import com.KimStock.application.service.StockCandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockCandleDataScheduler {

    private final StockCandleService stockCandleService;

    /**
     * 애플리케이션 시작 시 삼성전자 일봉 데이터 자동 저장
     */
//    @EventListener(ApplicationReadyEvent.class)
    public void initSamsungDayCandles() {
        log.info("애플리케이션 시작 - 삼성전자 일봉 데이터 초기화 시작");
        
        stockCandleService.saveSamsungDayCandles()
                .subscribe(
                        null,
                        error -> log.error("삼성전자 일봉 데이터 초기화 실패: {}", error.getMessage()),
                        () -> log.info("삼성전자 일봉 데이터 초기화 완료")
                );
    }
}
