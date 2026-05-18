package com.example.demo.api.stock.mapper;

import com.example.demo.api.stock.dto.StockResponseDto.StockPriceResponse;
import com.example.demo.domain.stock.entity.Stock;

public class StockConverter {

    public static StockPriceResponse toStockPriceResponse(Stock stock) {
        return StockPriceResponse.builder()
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .openPrice(stock.getOpenPrice())
                .closePrice(stock.getClosePrice())
                .inquiryDate(stock.getInquiryDate())
                .build();
    }
}
