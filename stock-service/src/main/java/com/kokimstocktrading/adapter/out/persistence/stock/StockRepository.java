package com.kokimstocktrading.adapter.out.persistence.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, String> {

    @Query("SELECT s FROM StockEntity s WHERE s.marketCode = :marketCode")
    List<StockEntity> findByMarketCode(@Param("marketCode") String marketCode);

    @Query("SELECT s FROM StockEntity s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<StockEntity> findByNameContaining(@Param("name") String name);

    @Query("SELECT s FROM StockEntity s WHERE s.code LIKE %:code%")
    List<StockEntity> findByCodeContaining(@Param("code") String code);

    @Query("SELECT s FROM StockEntity s WHERE LOWER(s.name) = LOWER(:name)")
    List<StockEntity> findByNameIgnoreCase(@Param("name") String name);

    /**
     * UPSERT를 위한 네이티브 쿼리
     * 기존 데이터가 있으면 업데이트, 없으면 삽입
     */
    @Modifying
    @Query(value = """
            INSERT INTO stock_trading.stock (
                code, name, list_count, audit_info, reg_day, state, 
                market_code, market_name, up_name, up_size_name, 
                company_class_name, order_warning, nxt_enable
            ) VALUES (
                :#{#stock.code}, :#{#stock.name}, :#{#stock.listCount}, 
                :#{#stock.auditInfo}, :#{#stock.regDay}, :#{#stock.state},
                :#{#stock.marketCode}, :#{#stock.marketName}, :#{#stock.upName},
                :#{#stock.upSizeName}, :#{#stock.companyClassName}, 
                :#{#stock.orderWarning}, :#{#stock.nxtEnable}
            )
            ON CONFLICT (code) DO UPDATE SET
                name = EXCLUDED.name,
                list_count = EXCLUDED.list_count,
                audit_info = EXCLUDED.audit_info,
                reg_day = EXCLUDED.reg_day,
                state = EXCLUDED.state,
                market_code = EXCLUDED.market_code,
                market_name = EXCLUDED.market_name,
                up_name = EXCLUDED.up_name,
                up_size_name = EXCLUDED.up_size_name,
                company_class_name = EXCLUDED.company_class_name,
                order_warning = EXCLUDED.order_warning,
                nxt_enable = EXCLUDED.nxt_enable
            """, nativeQuery = true)
    void upsertStock(@Param("stock") StockEntity stock);

    /**
     * 배치 UPSERT를 위한 네이티브 쿼리
     */
    @Modifying
    @Query(value = """
            INSERT INTO stock_trading.stock (
                code, name, list_count, audit_info, reg_day, state, 
                market_code, market_name, up_name, up_size_name, 
                company_class_name, order_warning, nxt_enable
            ) 
            SELECT * FROM UNNEST(
                :codes, :names, :listCounts, :auditInfos, :regDays, :states,
                :marketCodes, :marketNames, :upNames, :upSizeNames,
                :companyClassNames, :orderWarnings, :nxtEnables
            ) AS t(
                code, name, list_count, audit_info, reg_day, state,
                market_code, market_name, up_name, up_size_name,
                company_class_name, order_warning, nxt_enable
            )
            ON CONFLICT (code) DO UPDATE SET
                name = EXCLUDED.name,
                list_count = EXCLUDED.list_count,
                audit_info = EXCLUDED.audit_info,
                reg_day = EXCLUDED.reg_day,
                state = EXCLUDED.state,
                market_code = EXCLUDED.market_code,
                market_name = EXCLUDED.market_name,
                up_name = EXCLUDED.up_name,
                up_size_name = EXCLUDED.up_size_name,
                company_class_name = EXCLUDED.company_class_name,
                order_warning = EXCLUDED.order_warning,
                nxt_enable = EXCLUDED.nxt_enable
            """, nativeQuery = true)
    void batchUpsertStocks(
            @Param("codes") String[] codes,
            @Param("names") String[] names,
            @Param("listCounts") Long[] listCounts,
            @Param("auditInfos") String[] auditInfos,
            @Param("regDays") String[] regDays,
            @Param("states") String[] states,
            @Param("marketCodes") String[] marketCodes,
            @Param("marketNames") String[] marketNames,
            @Param("upNames") String[] upNames,
            @Param("upSizeNames") String[] upSizeNames,
            @Param("companyClassNames") String[] companyClassNames,
            @Param("orderWarnings") String[] orderWarnings,
            @Param("nxtEnables") Boolean[] nxtEnables
    );
}
