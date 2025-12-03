package com.kokimstocktrading.application.monitoring;

import com.kokimstocktrading.application.realtime.out.SubscribeRealTimeQuotePort;
import com.kokimstocktrading.domain.monitoring.PriceCondition;
import com.kokimstocktrading.domain.realtime.RealTimeQuote;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitorPriceService {

  private final SubscribeRealTimeQuotePort subscribeRealTimeQuotePort;

  // 종목별 가격 조건 리스트 저장 (종목코드 -> 조건 리스트)
  private final Map<String, List<PriceCondition>> stockConditions = new ConcurrentHashMap<>();

  // 조건 ID로 빠른 검색을 위한 맵 (조건ID -> 조건 객체)
  private final Map<UUID, PriceCondition> conditionById = new ConcurrentHashMap<>();

  // 모니터링 구독 관리
  private final Map<String, Disposable> monitoringSubscriptions = new ConcurrentHashMap<>();

  /**
   * 가격 조건 등록
   *
   * @param condition 가격 조건 도메인 모델
   * @return 등록된 조건 (ID 포함)
   */
  public PriceCondition registerPriceCondition(PriceCondition condition) {
    String stockCode = condition.getStockCode();

    stockConditions.computeIfAbsent(stockCode, k -> new ArrayList<>()).add(condition);
    conditionById.put(condition.getId(), condition);

    log.info("가격 조건 등록: {}", condition);
    startMonitoring(condition.getStockCode());
    return condition;
  }

  /**
   * 조건 ID로 조건 삭제
   */
  public boolean removePriceCondition(UUID conditionId) {
    PriceCondition condition = conditionById.remove(conditionId);
    if (condition == null) {
      log.warn("존재하지 않는 조건 ID: {}", conditionId);
      return false;
    }

    String stockCode = condition.getStockCode();
    List<PriceCondition> conditions = stockConditions.get(stockCode);

    if (conditions != null) {
      boolean removed = conditions.remove(condition);
      if (removed) {
        log.info("가격 조건 삭제: {}", condition);

        // 조건이 모두 삭제되면 해당 종목 모니터링 중지
        if (conditions.isEmpty()) {
          stockConditions.remove(stockCode);
          stopMonitoring(stockCode);
        }
        return true;
      }
    }

    return false;
  }

  /**
   * 종목의 모든 조건 삭제
   */
  public int removeAllConditions(String stockCode) {
    List<PriceCondition> conditions = stockConditions.remove(stockCode);
    if (conditions == null || conditions.isEmpty()) {
      return 0;
    }

    // ID 맵에서도 제거
    for (PriceCondition condition : conditions) {
      conditionById.remove(condition.getId());
    }

    stopMonitoring(stockCode);
    log.info("종목 {} 모든 조건 삭제: {}개", stockCode, conditions.size());
    return conditions.size();
  }

  /**
   * 조건 ID로 조건 조회
   */
  public Optional<PriceCondition> getCondition(UUID conditionId) {
    return Optional.ofNullable(conditionById.get(conditionId));
  }

  /**
   * 종목별 조건 리스트 조회
   */
  public List<PriceCondition> getConditions(String stockCode) {
    List<PriceCondition> conditions = stockConditions.get(stockCode);
    return conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
  }

  /**
   * 모든 조건 조회
   */
  public List<PriceCondition> getAllConditions() {
    return new ArrayList<>(conditionById.values());
  }

  /**
   * 종목별 등록된 조건 개수 조회
   */
  public int getConditionCount(String stockCode) {
    List<PriceCondition> conditions = stockConditions.get(stockCode);
    return conditions != null ? conditions.size() : 0;
  }

  /**
   * 전체 등록된 조건 개수 조회
   */
  public int getTotalConditionCount() {
    return conditionById.size();
  }

  /**
   * 모니터링 중인 종목 리스트 조회
   */
  public Set<String> getMonitoringStocks() {
    return new HashSet<>(stockConditions.keySet());
  }

  /**
   * 모니터링 시작
   */
  public void startMonitoring() {
    if (stockConditions.isEmpty()) {
      log.warn("등록된 가격 조건이 없습니다.");
      return;
    }

    List<String> stockCodes = List.copyOf(stockConditions.keySet());
    log.info("가격 모니터링 시작: 대상 종목={}, 총 조건 수={}", stockCodes, getTotalConditionCount());

    Flux<RealTimeQuote> realTimeQuoteFlux = subscribeRealTimeQuotePort.subscribeStockQuote(
        stockCodes);

    Disposable subscription = realTimeQuoteFlux
        .doOnNext(this::checkPriceConditions)
        .doOnError(error -> log.error("가격 모니터링 중 오류 발생", error))
        .subscribe();

    // 전체 모니터링 구독 저장
    monitoringSubscriptions.put("ALL", subscription);
  }

  /**
   * 개별 종목 모니터링 시작
   */
  public void startMonitoring(String stockCode) {
    if (!stockConditions.containsKey(stockCode)) {
      log.warn("종목 {}에 대한 가격 조건이 등록되지 않았습니다.", stockCode);
      return;
    }

    int conditionCount = getConditionCount(stockCode);
    log.info("개별 가격 모니터링 시작: 종목={}, 조건 수={}", stockCode, conditionCount);

    Flux<RealTimeQuote> realTimeQuoteFlux = subscribeRealTimeQuotePort.subscribeStockQuote(
        List.of(stockCode));

    Disposable subscription = realTimeQuoteFlux
        .filter(quote -> stockCode.equals(quote.item()))
        .doOnNext(this::checkPriceConditions)
        .doOnError(error -> log.error("종목 {} 가격 모니터링 중 오류 발생", stockCode, error))
        .subscribe();

    monitoringSubscriptions.put(stockCode, subscription);
  }

  /**
   * 가격 조건들 체크 (여러 조건 처리)
   */
  private void checkPriceConditions(RealTimeQuote quote) {
    String stockCode = quote.item();
    List<PriceCondition> conditions = stockConditions.get(stockCode);

    if (conditions == null || conditions.isEmpty()) {
      return;
    }

    try {
      double currentPrice = Double.parseDouble(quote.currentPrice());

      // 달성된 조건들을 수집
      List<PriceCondition> achievedConditions = new ArrayList<>();

      for (PriceCondition condition : conditions) {
        if (condition.isAchieved(currentPrice)) {
          log.info("가격 조건 달성! 조건={}, 현재가={}", condition, currentPrice);

          // 콜백을 비동기로 실행 (다른 조건 체크를 blocking하지 않도록)
          Mono.fromRunnable(condition::executeCallback)
              .subscribeOn(Schedulers.boundedElastic())
              .subscribe(
                  unused -> log.debug("조건 {} 콜백 실행 완료", condition.getId()),
                  error -> log.error("조건 {} 콜백 실행 중 오류", condition.getId(), error)
              );

          achievedConditions.add(condition);
        }
      }

      // 달성된 조건들 제거
      if (!achievedConditions.isEmpty()) {
        conditions.removeAll(achievedConditions);

        // ID 맵에서도 제거
        for (PriceCondition condition : achievedConditions) {
          conditionById.remove(condition.getId());
        }

        log.info("종목 {} - {}개 조건 달성 후 제거, 남은 조건: {}개",
            stockCode, achievedConditions.size(), conditions.size());

        // 모든 조건이 달성되면 해당 종목 모니터링 중지
        if (conditions.isEmpty()) {
          stockConditions.remove(stockCode);
          stopMonitoring(stockCode);
        }
      }

    } catch (NumberFormatException e) {
      log.warn("가격 파싱 실패: 종목={}, 가격={}", stockCode, quote.currentPrice());
    }
  }

  /**
   * 특정 종목 모니터링 중지
   */
  public void stopMonitoring(String stockCode) {
    Disposable subscription = monitoringSubscriptions.remove(stockCode);
    if (subscription != null && !subscription.isDisposed()) {
      subscription.dispose();
      log.info("종목 {} 모니터링 중지", stockCode);
    }

    // 조건들도 제거
    removeAllConditions(stockCode);
  }

  /**
   * 전체 모니터링 중지
   */
  public void stopAllMonitoring() {
    monitoringSubscriptions.values().forEach(subscription -> {
      if (!subscription.isDisposed()) {
        subscription.dispose();
      }
    });

    monitoringSubscriptions.clear();
    stockConditions.clear();
    conditionById.clear();
    log.info("전체 가격 모니터링 중지");
  }
}
