package com.KimStock.adapter.out.persistence.stock;

import com.KimStock.domain.model.Stock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock")
public class StockEntity implements Persistable<String> {

    @Id
    private String code;

    private String name;
    private Long listCount;
    private String auditInfo;
    private String regDay;
    private String state;
    private String marketCode;
    private String marketName;
    private String upName;
    private String upSizeName;
    private String companyClassName;
    private String orderWarning;
    private boolean nxtEnable;

    @Transient
    private boolean newEntity = true;

    // 정적 팩토리 메소드
    public static StockEntity of(Stock stock) {
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
                .isNxtEnable(this.nxtEnable)
                .build();
    }

    @Override
    public String getId() {
        return code;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    // 엔티티가 저장된 후 호출되어 새 엔티티가 아님을 표시
    public void setAsExisting() {
        this.newEntity = false;
    }
}
