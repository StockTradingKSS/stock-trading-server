package com.KimStock.application.port.out;

import com.KimStock.domain.model.Stock;

import java.util.List;

public interface SaveStockListPort {
    void saveStockList(List<Stock> stockList);
}
