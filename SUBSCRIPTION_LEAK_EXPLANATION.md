# Subscription 누적 문제 분석

## 실제 발생한 문제

```
2025-12-02T07:50:01 INFO - 이평선 주기적 업데이트 시작
  - 조건 ID: 2fd1801b-62b7-44a0-9615-acc40d715cda
  - 간격: PT1H (1시간)
  - 첫 실행까지: 598457ms (약 10분, 08:00:00 정각)

🔥 문제: 조건은 1개인데 8시에 37개 이상 업데이트 발생!
```

## 기존 로직 (❌ 문제)

```java
Disposable scheduler = Flux.defer(() ->
        Mono.fromRunnable(() ->
            updateMovingAverageCondition(condition)
                .doOnError(...)
                .onErrorContinue(...)
                .subscribe()  // ❌ 여기서 subscription 생성!
        )
    ).repeat()
    .delayElements(Duration.ofMillis(periodMs))
    .delaySubscription(Duration.ofMillis(initialDelayMs))
    .subscribeOn(Schedulers.fromExecutor(this.scheduler))
    .subscribe();
```

### 🔥 진짜 문제: Mono.fromRunnable이 즉시 완료됨

```
타임라인:

0ms:    defer() → Mono.fromRunnable 실행
        → () -> { updateMA().subscribe() } 실행
        → subscribe() 호출 [Sub-1 생성]
        → Mono.fromRunnable 즉시 완료! ✓

0ms:    repeat() 감지: "완료됐네? 다시 구독!"
        → defer() → Mono.fromRunnable 실행
        → subscribe() 호출 [Sub-2 생성]
        → Mono.fromRunnable 즉시 완료! ✓

0ms:    repeat() → defer() → subscribe() [Sub-3 생성] → 완료! ✓
0ms:    repeat() → defer() → subscribe() [Sub-4 생성] → 완료! ✓
...
1ms:    [Sub-1000 생성]
10ms:   [Sub-10000 생성] 🔥🔥🔥

100ms:  delayElements가 적용되려 하지만 이미 수만 개 생성됨!
```

**왜 이렇게 빠르냐?**
- `Mono.fromRunnable`은 **동기 작업**입니다
- `() -> { mono.subscribe() }`는 단순히 subscribe() 메서드 호출만 하고 끝
- 비동기 작업(updateMA)이 완료되길 기다리지 않음!
- `repeat()`는 Mono가 완료되면 즉시 재구독
- **무한 루프처럼 폭발적으로 증가!**

## 타임라인: subscription 누적 과정

```
07:50:01 - 조건 등록
         └─> 외부 Flux 구독 시작

08:00:00 - 첫 번째 실행 (initialDelay 후)
         └─> defer() 호출
         └─> Mono.fromRunnable 실행
         └─> updateMovingAverageCondition().subscribe()  [Sub-1 생성]
         └─> delayElements(1시간)

09:00:00 - repeat() → 두 번째 실행
         └─> defer() 호출
         └─> Mono.fromRunnable 실행
         └─> updateMovingAverageCondition().subscribe()  [Sub-2 생성]
         └─> ⚠️ Sub-1은 dispose 안됨! 계속 살아있음
         └─> delayElements(1시간)

10:00:00 - repeat() → 세 번째 실행
         └─> [Sub-3 생성]
         └─> ⚠️ Sub-1, Sub-2 계속 살아있음

...

17:00:00 (8시간 후) - repeat() → 아홉 번째 실행
         └─> [Sub-9 생성]
         └─> 🔥 총 9개의 subscription이 메모리에 누적!

18:00:00 - 정각 업데이트 트리거
         └─> Sub-1: updateMovingAverage() 실행 → PriceCondition 업데이트
         └─> Sub-2: updateMovingAverage() 실행 → PriceCondition 업데이트
         └─> Sub-3: updateMovingAverage() 실행 → PriceCondition 업데이트
         └─> ...
         └─> Sub-9: updateMovingAverage() 실행 → PriceCondition 업데이트

         결과: 동일한 조건이 9번 업데이트됨!
         로그: "이평선 조건 업데이트" x 9번 출력
```

## 왜 37개가 발생했나?

실제로는 **몇 초 만에 수만 개**가 생성될 수 있습니다!

서버에서 37개로 제한된 이유 (추측):
1. **스레드풀 제한**: `subscribeOn(Schedulers.fromExecutor(scheduler))`
   - ScheduledExecutorService가 4개 스레드풀로 제한
   - 큐가 가득 차면 더 이상 생성 안됨

2. **시스템 리소스**: CPU/메모리 한계에 도달

3. **Reactor 내부 제한**: backpressure나 내부 버퍼 제한

하지만 핵심은:
- `Mono.fromRunnable(() -> mono.subscribe())`는 즉시 완료
- `repeat()`가 무한정 빠르게 반복
- **1초에 수천~수만 개 생성 가능**
- 실제 서버에서는 어떤 제한으로 37개에서 멈춤 (운이 좋았음!)

## 코드 레벨 문제점

### 1. 중첩 subscription
```java
.subscribe()  // 외부 subscription
  └─> repeat()
      └─> Mono.fromRunnable(() -> {
              updateMA().subscribe()  // ❌ 내부 subscription (누적!)
          })
```

### 2. Dispose 되지 않는 내부 subscription
- 외부 subscription을 dispose해도
- 내부에서 생성된 subscription들은 **살아있음**
- 메모리 누수 + 중복 실행

## 수정된 로직 (✅ 해결)

```java
Disposable scheduler = Flux.interval(
        Duration.ofMillis(initialDelayMs),
        Duration.ofMillis(periodMs),
        Schedulers.fromExecutor(this.scheduler)
    )
    .flatMap(tick -> updateMovingAverageCondition(condition)
        .doOnError(...)
        .onErrorResume(error -> {
            log.warn("업데이트 오류: {}", error.getMessage());
            return Mono.empty();
        })
    )
    .subscribe();
```

## 수정 후 타임라인

```
07:50:01 - 조건 등록
         └─> Flux.interval 구독 시작 [단 1개의 subscription]

08:00:00 - tick=0 (첫 실행)
         └─> updateMovingAverageCondition() 실행
         └─> PriceCondition 업데이트
         └─> ✅ 1번만 실행!

09:00:00 - tick=1 (두 번째)
         └─> 동일한 subscription에서 실행
         └─> ✅ 1번만 실행!

10:00:00 - tick=2
         └─> ✅ 1번만 실행!

...

18:00:00 - tick=10
         └─> ✅ 정확히 1번만 실행!
```

## 핵심 차이점

| 항목 | 기존 (문제) | 수정 (해결) |
|------|------------|------------|
| Subscription 수 | 시간당 1개씩 누적 (37시간 = 37개) | 항상 1개 |
| 업데이트 횟수 | 누적된 만큼 중복 실행 (37번) | 정확히 1번 |
| 메모리 | 누수 발생 | 누수 없음 |
| Dispose | 내부 sub는 dispose 안됨 | 모두 정상 dispose |

## 테스트로 확인

`DynamicConditionServiceTest.java`에 추가된 테스트:

1. `oldLogic_subscriptionAccumulation_causeMultipleUpdates`
   - 기존 로직의 문제 재현
   - repeat(5) → 6개 subscription 생성 확인

2. `newLogic_singleSubscription_updatesCorrectly`
   - 수정된 로직 검증
   - 단 1개 subscription으로 정확한 횟수 실행

## 결론

**문제 원인**: `repeat()` + `Mono.fromRunnable(() -> mono.subscribe())`
- 반복할 때마다 새로운 subscription 생성
- 이전 subscription들이 dispose 안되고 계속 살아있음
- 누적된 subscription들이 모두 실행됨

**해결 방법**: `Flux.interval()` + `flatMap()`
- 단일 subscription만 유지
- flatMap으로 비동기 작업 체인
- 정확히 1번만 실행
