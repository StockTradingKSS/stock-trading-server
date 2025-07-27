package com.kokimstocktrading.application.stock.port.out;

import com.kokimstocktrading.domain.stock.Stock;

import java.util.List;

public interface SaveStockListPort {
    void saveStockList(List<Stock> stockList);
}
