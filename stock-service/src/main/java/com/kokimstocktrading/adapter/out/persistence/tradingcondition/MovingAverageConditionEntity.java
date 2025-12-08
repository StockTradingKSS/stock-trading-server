package com.kokimstocktrading.adapter.out.persistence.tradingcondition;

import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.ConditionStatus;
import com.kokimstocktrading.domain.monitoring.MovingAverageCondition;
import com.kokimstocktrading.domain.monitoring.TouchDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 이평선 조건 엔티티 이동평균선 기반 거래 조건을 저장
 */
@Entity
@Table(name = "moving_average_conditions", indexes = {
    @Index(name = "idx_stock_code", columnList = "stock_code"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_stock_code_status", columnList = "stock_code, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovingAverageConditionEntity {

  /**
   * 조건 고유 ID (UUID)
   */
  @Id
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;

  /**
   * 종목 코드 (예: 005930)
   */
  @Column(name = "stock_code", nullable = false, length = 20)
  private String stockCode;

  /**
   * 이평선 기간 (예: 20일선이면 20)
   */
  @Column(name = "period", nullable = false)
  private Integer period;

  /**
   * 캔들 간격 (분/일/주/월/년)
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "interval", nullable = false, length = 20)
  private CandleInterval interval;

  /**
   * 터치 방향 (상향/하향 돌파)
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "touch_direction", nullable = false, length = 20)
  private TouchDirection touchDirection;

  /**
   * 조건 설명
   */
  @Column(name = "description", length = 500)
  private String description;

  /**
   * 조건 상태 (START: 감시중, SUCCESS: 조건 달성)
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ConditionStatus status = ConditionStatus.START;

  /**
   * 생성 시각
   */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * 수정 시각
   */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * 정적 팩토리 메서드 - 새 조건 생성
   */
  public static MovingAverageConditionEntity create(
      UUID id,
      String stockCode,
      Integer period,
      CandleInterval interval,
      TouchDirection touchDirection,
      String description,
      ConditionStatus status
  ) {
    MovingAverageConditionEntity entity = new MovingAverageConditionEntity();
    entity.id = id;
    entity.stockCode = stockCode;
    entity.period = period;
    entity.interval = interval;
    entity.touchDirection = touchDirection;
    entity.description = description;
    entity.status = status;
    return entity;
  }

  /**
   * 조건을 SUCCESS 상태로 변경
   */
  public void markAsSuccess() {
    this.status = ConditionStatus.SUCCESS;
  }

  /**
   * 조건을 START 상태로 변경
   */
  public void markAsStart() {
    this.status = ConditionStatus.START;
  }

  /**
   * 엔티티 → 도메인 변환
   *
   * @param callback 조건 만족 시 실행할 콜백 (DB에 저장 불가하므로 별도 주입)
   * @return 도메인 객체
   */
  public MovingAverageCondition toDomain(Runnable callback) {
    return new MovingAverageCondition(
        this.id,
        this.stockCode,
        this.period,
        this.interval,
        this.touchDirection,
        callback,
        this.description
    );
  }
}
