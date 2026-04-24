package com.example.demo.domain.favoriteStock.service;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;

public interface FavoriteStockCommandService {
    Long addFavoriteStock(FavoriteStock favoriteStock);
    boolean turnOnFavoriteStockAlarm(Long favoriteStockId);
    boolean turnOffFavoriteStockAlarm(Long favoriteStockId);
    Void deleteFavoriteStock(Long favoriteStockId);
}
