package com.kokimstocktrading.adapter.out.persistence.stock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
   * 배치 UPSERT를 위한 네이티브 쿼리
   * audit_info는 TEXT로 CAST하여 bytea 타입 문제 해결
   */
  @Modifying
  @Query(value = """
    INSERT INTO stock (
        code,
        name,
        list_count,
        audit_info,
        reg_day,
        state,
        market_code,
        market_name,
        up_name,
        up_size_name,
        company_class_name,
        order_warning,
        nxt_enable
    )
    SELECT 
        t.code,
        t.name,
        t.list_count,
        t.audit_info,
        t.reg_day,
        t.state,
        t.market_code,
        t.market_name,
        t.up_name,
        t.up_size_name,
        t.company_class_name,
        t.order_warning,
        t.nxt_enable
    FROM UNNEST(
        CAST(:codes AS text[]),
        CAST(:names AS text[]),
        CAST(:listCounts AS bigint[]),
        CAST(:auditInfos AS text[]),
        CAST(:regDays AS text[]),
        CAST(:states AS text[]),
        CAST(:marketCodes AS text[]),
        CAST(:marketNames AS text[]),
        CAST(:upNames AS text[]),
        CAST(:upSizeNames AS text[]),
        CAST(:companyClassNames AS text[]),
        CAST(:orderWarnings AS text[]),
        CAST(:nxtEnables AS boolean[])
    ) AS t(
        code,
        name,
        list_count,
        audit_info,
        reg_day,
        state,
        market_code,
        market_name,
        up_name,
        up_size_name,
        company_class_name,
        order_warning,
        nxt_enable
    )
    ON CONFLICT (code) DO UPDATE SET
        name               = EXCLUDED.name,
        list_count         = EXCLUDED.list_count,
        audit_info         = EXCLUDED.audit_info,
        reg_day            = EXCLUDED.reg_day,
        state              = EXCLUDED.state,
        market_code        = EXCLUDED.market_code,
        market_name        = EXCLUDED.market_name,
        up_name            = EXCLUDED.up_name,
        up_size_name       = EXCLUDED.up_size_name,
        company_class_name = EXCLUDED.company_class_name,
        order_warning      = EXCLUDED.order_warning,
        nxt_enable         = EXCLUDED.nxt_enable
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
