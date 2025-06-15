package com.KimStock.adapter.out.external.chart;

public class StockCodeParser {
    public static String getOriginalStockCode(String stockCode){
        return stockCode.split("_")[0];
    }
}
