package com.example.demo.api.favoriteStock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
