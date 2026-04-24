package com.example.demo.api.favoriteStock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class FavoriteStockResponseDto {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteStockDto {
        private Long favoriteStockId;
        private Long userId;
        private String stockCode;
        private String ticker;
        private Boolean isAlertEnabled;
    }

    @Data
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
