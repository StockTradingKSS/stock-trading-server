package com.KimStock.adapter.in.scheduler;

import com.KimStock.application.port.in.RefreshStockUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockBatchScheduler {
    private final RefreshStockUseCase refreshStockUseCase;

    @Scheduled(cron = "0 30 7 * * *", zone = "Asia/Seoul")
    public void saveStockScheduler() {
        refreshStockUseCase.refreshStock()
                        .subscribe(isSuccess -> {
                                String message = isSuccess ? "Success" : "Failed";
                                log.info("Refresh stock {} at time {}",message, LocalDateTime.now());
                        });
    }
}
