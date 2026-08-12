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
        /** 시세 기준 거래일. 휴장일에 실행하면 직전 거래일이 담긴다. */
        private LocalDate tradeDate;
        private int addedCount;
        private int updatedCount;
        /** 코스피200에서 편출되어 비활성 처리된 종목 수. 데이터는 삭제되지 않는다. */
        private int deactivatedCount;
        /** 다시 편입되어 활성으로 되돌린 종목 수. */
        private int reactivatedCount;
        /** 시가·종가가 갱신된 종목 수. */
        private int priceUpdatedCount;
        /** Lambda가 시세를 구하지 못해 제외한 종목 코드. */
        private List<String> excludedStockCodes;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItemResponse {
        private String stockCode;
        private String stockName;
        /** 코스피200 편입 여부. false면 편출된 종목으로 시세가 더 이상 갱신되지 않는다. */
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
