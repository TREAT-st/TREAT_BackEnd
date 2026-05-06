package com.example.demo.domain.favoriteStock.service;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;

public interface FavoriteStockCommandService {
    Long addFavoriteStock(FavoriteStock favoriteStock);
    void updateFavoriteStockAlarm(Long favoriteStockId, boolean isEnabled);
    void deleteFavoriteStock(Long favoriteStockId);
}
