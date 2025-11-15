package com.kokimstocktrading.adapter.out.persistence.entity;

import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.TouchDirection;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이평선 조건 엔티티
 * 이동평균선 기반 거래 조건을 저장
 */
@Entity
@Table(name = "moving_average_conditions", indexes = {
        @Index(name = "idx_stock_code", columnList = "stock_code"),
        @Index(name = "idx_is_active", columnList = "is_active"),
        @Index(name = "idx_stock_code_active", columnList = "stock_code, is_active")
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
     * 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
            String stockCode,
            Integer period,
            CandleInterval interval,
            TouchDirection touchDirection,
            String description
    ) {
        MovingAverageConditionEntity entity = new MovingAverageConditionEntity();
        entity.id = UUID.randomUUID();
        entity.stockCode = stockCode;
        entity.period = period;
        entity.interval = interval;
        entity.touchDirection = touchDirection;
        entity.description = description;
        entity.isActive = true;
        return entity;
    }

    /**
     * 조건 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 조건 활성화
     */
    public void activate() {
        this.isActive = true;
    }
}
