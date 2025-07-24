package com.kokimstocktrading.application.port.out;

import com.kokimstocktrading.domain.model.Stock;

import java.util.List;

public interface SaveStockListPort {
    void saveStockList(List<Stock> stockList);
}
