package com.example.demo.api.favoriteStock.service;

import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.favoriteStock.service.FavoriteStockCommandService;
import com.example.demo.domain.favoriteStock.service.FavoriteStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class FavoriteStockUseCase {
    private final FavoriteStockCommandService favoriteStockCommandService;
    private final FavoriteStockQueryService favoriteStockQueryService;

    public Long addFavoriteStock(FavoriteStock favoriteStock) {
        return favoriteStockCommandService.addFavoriteStock(favoriteStock);
    }

    public Page<FavoriteStock> getFavoriteStockPageByUserId(Long userId, Pageable pageable) {
        return favoriteStockQueryService.getUserFavoriteStockListByPage(userId, pageable);
    }

    public Void deleteFavoriteStock(Long favoriteStockId) {
        return favoriteStockCommandService.deleteFavoriteStock(favoriteStockId);
    }
}
