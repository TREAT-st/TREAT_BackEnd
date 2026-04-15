package com.example.demo.domain.favoriteStock.service;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;

public interface FavoriteCommandService {
    FavoriteStock addFavoriteStock(FavoriteStock favoriteStock);
    boolean turnOnFavoriteStockAlarm(Long favoriteStockId);
    boolean turnOffFavoriteStockAlarm(Long favoriteStockId);
    Long deleteFavoriteStock(Long favoriteStockId);
}
