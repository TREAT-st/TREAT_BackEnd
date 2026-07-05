package com.example.demo.api.stock.mapper;

import com.example.demo.api.stock.dto.StockResponseDto.StockItemResponse;
import com.example.demo.api.stock.dto.StockResponseDto.StockPageResponse;
import com.example.demo.api.stock.dto.StockResponseDto.StockPriceResponse;
import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

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

    public static StockItemResponse toStockItemResponse(Stock stock) {
        return StockItemResponse.builder()
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .build();
    }

    public static List<StockItemResponse> toStockItemResponseList(List<Stock> stocks) {
        return stocks.stream()
                .map(StockConverter::toStockItemResponse)
                .collect(Collectors.toList());
    }

    public static StockPageResponse toStockPageResponse(Page<Stock> page) {
        List<StockItemResponse> content = toStockItemResponseList(page.getContent());
        return StockPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
