package com.kokimstocktrading.adapter.out.persistence.entity;

import com.kokimstocktrading.domain.candle.CandleInterval;
import com.kokimstocktrading.domain.monitoring.TouchDirection;
import com.kokimstocktrading.domain.monitoring.TrendLineCondition;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 추세선 조건 엔티티
 * 추세선 기반 거래 조건을 저장
 */
@Entity
@Table(name = "trend_line_conditions", indexes = {
        @Index(name = "idx_tl_stock_code", columnList = "stock_code"),
        @Index(name = "idx_tl_is_active", columnList = "is_active"),
        @Index(name = "idx_tl_stock_code_active", columnList = "stock_code, is_active")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrendLineConditionEntity {

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
     * 추세선 끝점 날짜
     */
    @Column(name = "to_date", nullable = false)
    private LocalDateTime toDate;

    /**
     * 추세선 기울기
     */
    @Column(name = "slope", nullable = false, precision = 19, scale = 4)
    private BigDecimal slope;

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
    public static TrendLineConditionEntity create(
            String stockCode,
            LocalDateTime toDate,
            BigDecimal slope,
            CandleInterval interval,
            TouchDirection touchDirection,
            String description
    ) {
        TrendLineConditionEntity entity = new TrendLineConditionEntity();
        entity.id = UUID.randomUUID();
        entity.stockCode = stockCode;
        entity.toDate = toDate;
        entity.slope = slope;
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

    /**
     * 엔티티 → 도메인 변환
     *
     * @param callback 조건 만족 시 실행할 콜백 (DB에 저장 불가하므로 별도 주입)
     * @return 도메인 객체
     */
    public TrendLineCondition toDomain(Runnable callback) {
        return new TrendLineCondition(
                this.id,
                this.stockCode,
                this.toDate,
                this.slope,
                this.interval,
                this.touchDirection,
                callback,
                this.description
        );
    }
}
