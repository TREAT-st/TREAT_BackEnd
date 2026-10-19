package com.example.demo.api.favoriteStock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FavoriteStockResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteStockDto {
        private Long favoriteStockId;
        private Long userId;
        private String stockCode;
        private String stockName;
        private Boolean isAlertEnabled;
        /**
         * tradeDate의 종가. 실시간 시세가 아니다.
         * stock에 없는 종목이거나 아직 한 번도 시세가 반영되지 않았으면 null.
         * 최근 동기화에서 시세를 못 받은 종목은 마지막으로 반영된 값이 남아 있고, tradeDate가 그만큼 이전이다.
         */
        private BigDecimal currentPrice;
        /** 직전 거래일 종가 대비 등락폭(원). 하락이면 음수. */
        private BigDecimal priceChange;
        /** 직전 거래일 종가 대비 등락률(%). 하락이면 음수. */
        private BigDecimal changeRate;
        /** 위 시세의 기준 거래일. */
        private LocalDate tradeDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteStockPageResponse {
        private List<FavoriteStockDto> content;
        private int page;
        private int totalPages;
        private boolean hasNext;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistinctStockResponse {
        private String stockCode;
        private String stockName;
    }
}
