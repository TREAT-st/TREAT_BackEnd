package com.example.demo.api.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class StockResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncStocksResponse {
        private LocalDate tradeDate;
        private int addedCount;
        private int updatedCount;
        private int deactivatedCount;
        private int reactivatedCount;
        private int priceUpdatedCount;
        /**
         * 구성종목이지만 종목명을 받지 못해 목록에 반영하지 못한 종목.
         * 편출인지 일시적 실패인지 알 수 없어 활성 상태를 건드리지 않는다.
         */
        private List<String> unresolvedStockCodes;
        /**
         * 목록에는 반영됐지만 시세를 받지 못한 종목(거래정지 등).
         * 종목 자체는 정상 편입 상태이며 시가·종가만 이전 값으로 남는다.
         */
        private List<String> priceUnavailableStockCodes;

        // 시세는 받았지만 DB에 반영하지 못한 종목. 비어 있지 않으면 동기화 정합성 이상 신호다.
        private List<String> priceUpdateSkippedStockCodes;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItemResponse {
        private String stockCode;
        private String stockName;
        private Boolean isActive;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockOpenAndClosePriceResponse {
        private String stockCode;
        private String stockName;
        private BigDecimal openPrice;
        private BigDecimal closePrice;
        private LocalDate tradeDate;
        private Boolean isActive;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockPageResponse {
        private List<StockItemResponse> content;
        private int page;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
    }
}
