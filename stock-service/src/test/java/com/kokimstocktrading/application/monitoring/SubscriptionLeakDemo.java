package com.kokimstocktrading.application.monitoring;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 기존 로직의 subscription 누적 문제를 시각적으로 보여주는 데모
 */
@Slf4j
public class SubscriptionLeakDemo {

  public static void main(String[] args) throws InterruptedException {
    log.info("========================================");
    log.info("기존 로직 (문제 있음): defer + Mono.fromRunnable(subscribe()) + repeat");
    log.info("========================================\n");

    AtomicInteger updateCount = new AtomicInteger(0);
    AtomicInteger activeSubscriptionCount = new AtomicInteger(0);

    // 기존 문제 로직
    Disposable problematicScheduler = Flux.defer(() -> {
          int deferCall = activeSubscriptionCount.get() + 1;
          log.info("▶ defer() 호출 #{} - 새로운 Mono.fromRunnable 생성", deferCall);

          return Mono.fromRunnable(() -> {
            // 비동기 작업을 내부에서 subscribe() (문제!)
            Mono.delay(Duration.ofMillis(10))
                .doOnSubscribe(s -> {
                  int count = activeSubscriptionCount.incrementAndGet();
                  log.warn("  🔥 새로운 subscription 생성! 활성: {}", count);
                })
                .doOnNext(tick -> {
                  int count = updateCount.incrementAndGet();
                  log.info("    ✓ 업데이트 실행 #{} (활성 subscription: {})",
                      count, activeSubscriptionCount.get());
                })
                .subscribe();  // ❌ 문제!
          });
        })
        .repeat(4)  // 4번 반복
        .delayElements(Duration.ofMillis(200))
        .subscribe();

    log.info("\n⏱ 1.5초 대기...\n");
    Thread.sleep(1500);

    problematicScheduler.dispose();

    log.info("\n========================================");
    log.info("📊 기존 로직 최종 결과:");
    log.info("  - 생성된 subscription 수: {}", activeSubscriptionCount.get());
    log.info("  - 총 업데이트 실행 횟수: {}", updateCount.get());
    log.info("  - 문제: repeat(4)면 5번 실행인데, subscription이 {} 개나 누적!",
        activeSubscriptionCount.get());
    log.info("========================================\n\n");

    Thread.sleep(500);

    // 새로운 로직
    log.info("========================================");
    log.info("새로운 로직 (해결): Flux.interval");
    log.info("========================================\n");

    AtomicInteger newUpdateCount = new AtomicInteger(0);
    AtomicInteger newSubscriptionCount = new AtomicInteger(0);

    Disposable correctScheduler = Flux.interval(
            Duration.ofMillis(100),
            Duration.ofMillis(200)
        )
        .doOnSubscribe(s -> {
          int count = newSubscriptionCount.incrementAndGet();
          log.info("▶ Flux.interval subscription 생성: {}", count);
        })
        .take(5)
        .flatMap(tick -> {
          log.info("  ⏰ tick #{} 발생", tick);
          return Mono.delay(Duration.ofMillis(10))
              .doOnNext(t -> {
                int count = newUpdateCount.incrementAndGet();
                log.info("    ✓ 업데이트 실행 #{}", count);
              });
        })
        .subscribe();

    log.info("\n⏱ 1.5초 대기...\n");
    Thread.sleep(1500);

    correctScheduler.dispose();

    log.info("\n========================================");
    log.info("📊 새로운 로직 최종 결과:");
    log.info("  - 생성된 subscription 수: {}", newSubscriptionCount.get());
    log.info("  - 총 업데이트 실행 횟수: {}", newUpdateCount.get());
    log.info("  - 해결: 단 1개의 subscription으로 정확히 5번만 실행!");
    log.info("========================================");
  }
}
