package com.kokimstocktrading.adapter.out.persistence.stock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
public class StockRepositoryCustomImpl implements StockRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  @Transactional
  public void batchUpsertStocks(
      String[] codes,
      String[] names,
      Long[] listCounts,
      String[] auditInfos,
      String[] regDays,
      String[] states,
      String[] marketCodes,
      String[] marketNames,
      String[] upNames,
      String[] upSizeNames,
      String[] companyClassNames,
      String[] orderWarnings,
      Boolean[] nxtEnables
  ) {
    String sql = """
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
        FROM UNNEST(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AS t(
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
        """;

    Session session = entityManager.unwrap(Session.class);
    session.doWork(connection -> {
      try (PreparedStatement ps = connection.prepareStatement(sql)) {
        // Create PostgreSQL arrays using the JDBC connection
        Array codesArray = connection.createArrayOf("text", codes);
        Array namesArray = connection.createArrayOf("text", names);
        Array listCountsArray = connection.createArrayOf("bigint", listCounts);
        Array auditInfosArray = connection.createArrayOf("text", auditInfos);
        Array regDaysArray = connection.createArrayOf("text", regDays);
        Array statesArray = connection.createArrayOf("text", states);
        Array marketCodesArray = connection.createArrayOf("text", marketCodes);
        Array marketNamesArray = connection.createArrayOf("text", marketNames);
        Array upNamesArray = connection.createArrayOf("text", upNames);
        Array upSizeNamesArray = connection.createArrayOf("text", upSizeNames);
        Array companyClassNamesArray = connection.createArrayOf("text", companyClassNames);
        Array orderWarningsArray = connection.createArrayOf("text", orderWarnings);
        Array nxtEnablesArray = connection.createArrayOf("boolean", nxtEnables);

        // Set parameters
        ps.setArray(1, codesArray);
        ps.setArray(2, namesArray);
        ps.setArray(3, listCountsArray);
        ps.setArray(4, auditInfosArray);
        ps.setArray(5, regDaysArray);
        ps.setArray(6, statesArray);
        ps.setArray(7, marketCodesArray);
        ps.setArray(8, marketNamesArray);
        ps.setArray(9, upNamesArray);
        ps.setArray(10, upSizeNamesArray);
        ps.setArray(11, companyClassNamesArray);
        ps.setArray(12, orderWarningsArray);
        ps.setArray(13, nxtEnablesArray);

        int rowsAffected = ps.executeUpdate();
        log.debug("Batch upsert affected {} rows", rowsAffected);

        // Free arrays
        codesArray.free();
        namesArray.free();
        listCountsArray.free();
        auditInfosArray.free();
        regDaysArray.free();
        statesArray.free();
        marketCodesArray.free();
        marketNamesArray.free();
        upNamesArray.free();
        upSizeNamesArray.free();
        companyClassNamesArray.free();
        orderWarningsArray.free();
        nxtEnablesArray.free();
      }
    });
  }
}
