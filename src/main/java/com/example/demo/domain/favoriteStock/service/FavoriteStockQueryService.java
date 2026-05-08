package com.example.demo.domain.favoriteStock.service;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface FavoriteStockQueryService {
    FavoriteStock getFavoriteStockById(Long id);
    Page<FavoriteStock> getUserFavoriteStockListByPage(Long userId, Pageable pageable);
    FavoriteStock getUserFavoriteStockByUserIdAndStockCode(Long userId, String stockCode);
    boolean existsFavoriteStockByUserIdAndStockCode(Long userId, String stockCode);
}
