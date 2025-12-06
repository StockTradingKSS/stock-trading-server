package com.kokimstocktrading.adapter.out.persistence.stock;

public interface StockRepositoryCustom {

  void batchUpsertStocks(
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
  );

}
