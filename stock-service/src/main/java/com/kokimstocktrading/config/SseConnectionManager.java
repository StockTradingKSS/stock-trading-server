package com.kokimstocktrading.config;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * SSE 연결 관리자
 * Graceful shutdown 시 모든 SSE 연결을 강제로 종료합니다.
 */
@Component
@Slf4j
public class SseConnectionManager {

  private final Sinks.Many<Void> shutdownSink = Sinks.many().multicast().onBackpressureBuffer();
  private final Flux<Void> shutdownSignal = shutdownSink.asFlux();

  /**
   * Shutdown signal을 구독할 수 있는 Flux를 반환
   */
  public Flux<Void> getShutdownSignal() {
    return shutdownSignal;
  }

  /**
   * 애플리케이션 종료 시 모든 SSE 연결을 강제 종료
   */
  @PreDestroy
  public void shutdown() {
    log.info("SSE 연결 관리자 종료 중 - 모든 활성 SSE 연결을 종료합니다");
    shutdownSink.tryEmitComplete();
    log.info("모든 SSE 연결 종료 신호 전송 완료");
  }
}
