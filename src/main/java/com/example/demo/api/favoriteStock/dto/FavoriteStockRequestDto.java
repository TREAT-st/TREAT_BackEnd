package com.example.demo.api.favoriteStock.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FavoriteStockRequestDto {
    private String stockCode;
    private String ticker;
}
