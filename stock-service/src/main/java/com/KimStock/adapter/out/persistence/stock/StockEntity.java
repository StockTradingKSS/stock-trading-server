package com.KimStock.adapter.out.persistence.stock;

import com.KimStock.domain.model.Stock;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock", indexes = {
        @Index(name = "idx_stock_market_code", columnList = "market_code"),
        @Index(name = "idx_stock_name", columnList = "name")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockEntity {

    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "list_count")
    private Long listCount;

    @Column(name = "audit_info")
    private String auditInfo;

    @Column(name = "reg_day", length = 20)
    private String regDay;

    @Column(name = "state", length = 20)
    private String state;

    @Column(name = "market_code", length = 20)
    private String marketCode;

    @Column(name = "market_name", length = 50)
    private String marketName;

    @Column(name = "up_name", length = 50)
    private String upName;

    @Column(name = "up_size_name", length = 50)
    private String upSizeName;

    @Column(name = "company_class_name", length = 50)
    private String companyClassName;

    @Column(name = "order_warning", length = 50)
    private String orderWarning;

    @Column(name = "nxt_enable")
    private boolean nxtEnable;

    // 정적 팩토리 메소드
    public static StockEntity from(Stock stock) {
        return StockEntity.builder()
                .code(stock.getCode())
                .name(stock.getName())
                .listCount(stock.getListCount())
                .auditInfo(stock.getAuditInfo())
                .regDay(stock.getRegDay())
                .state(stock.getState())
                .marketCode(stock.getMarketCode())
                .marketName(stock.getMarketName())
                .upName(stock.getUpName())
                .upSizeName(stock.getUpSizeName())
                .companyClassName(stock.getCompanyClassName())
                .orderWarning(stock.getOrderWarning())
                .nxtEnable(stock.isNxtEnable())
                .build();
    }

    // 도메인 변환 메소드
    public Stock toDomain() {
        return Stock.builder()
                .code(this.code)
                .name(this.name)
                .listCount(this.listCount)
                .auditInfo(this.auditInfo)
                .regDay(this.regDay)
                .state(this.state)
                .marketCode(this.marketCode)
                .marketName(this.marketName)
                .upName(this.upName)
                .upSizeName(this.upSizeName)
                .companyClassName(this.companyClassName)
                .orderWarning(this.orderWarning)
                .nxtEnable(this.nxtEnable)
                .build();
    }
}
