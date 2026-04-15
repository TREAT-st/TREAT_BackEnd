package com.example.demo.domain.favoriteStock.service;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteQueryService {
    Page<FavoriteStock> getUserFavoriteStockListByPage(Long userId, Pageable pageable);
    FavoriteStock getUserFavoriteStockByUserIdAndStockCode(Long userId, String stockCode);
    Boolean existsFavoriteStockByUserIdAndStockCode(Long userId, String stockCode);
}
