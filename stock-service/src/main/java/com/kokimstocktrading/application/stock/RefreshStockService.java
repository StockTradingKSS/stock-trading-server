package com.kokimstocktrading.application.stock;

import com.common.UseCase;
import com.kokimstocktrading.application.stock.port.in.RefreshStockUseCase;
import com.kokimstocktrading.application.stock.port.out.LoadStockListPort;
import com.kokimstocktrading.application.stock.port.out.SaveStockListPort;
import com.kokimstocktrading.domain.market.MarketType;
import com.kokimstocktrading.domain.stock.Stock;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RefreshStockService implements RefreshStockUseCase {

  private final SaveStockListPort saveStockListPort;
  private final LoadStockListPort loadStockListPort;

  @Override
  public boolean refreshStock() {
    try {
      // KOSPI와 KOSDAQ 종목 정보들을 외부 API 를 통해 받아온다.
      List<Stock> kosdaqStocks = loadStockListPort.loadStockInfoListBy(MarketType.KOSDAQ, null,
          null).block();
      List<Stock> kospiStocks = loadStockListPort.loadStockInfoListBy(MarketType.KOSPI, null, null)
          .block();

      // 두 리스트를 합쳐서 저장
      List<Stock> allStocks = new ArrayList<>();
      assert kosdaqStocks != null;
      assert kospiStocks != null;

      allStocks.addAll(kosdaqStocks);
      allStocks.addAll(kospiStocks);

      // 받아온 종목 정보를 DB에 저장한다.
      saveStockListPort.saveStockList(allStocks);

      log.info("Successfully refreshed stock information. Total stocks: {}", allStocks.size());
      return true;
    } catch (Exception e) {
      log.error("Failed to refresh stock information", e);
      return false;
    }
  }
}
