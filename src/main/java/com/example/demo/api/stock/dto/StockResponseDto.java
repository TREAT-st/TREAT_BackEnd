package com.example.demo.api.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StockResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockPriceResponse {
        private String stockCode;
        private String stockName;
        private BigDecimal openPrice;
        private BigDecimal closePrice;
        private LocalDate inquiryDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadExcelResponse {
        private String s3Uri;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportStockResponse {
        private int savedCount;
    }
}
