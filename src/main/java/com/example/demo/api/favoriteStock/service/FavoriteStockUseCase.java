package com.example.demo.api.favoriteStock.service;

import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.favoriteStock.exception.FavoriteStockHandler;
import com.example.demo.domain.favoriteStock.service.FavoriteStockCommandService;
import com.example.demo.domain.favoriteStock.service.FavoriteStockQueryService;
import com.example.demo.domain.user.entity.User;
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

    public Long updateFavoriteStockAlarm(User user, Long favoriteStockId, boolean isEnabled) {
        FavoriteStock favoriteStock = favoriteStockQueryService.getFavoriteStockById(favoriteStockId);

        if (favoriteStock.getUser().getId().equals(user.getId())) {
            favoriteStockCommandService.updateFavoriteStockAlarm(favoriteStockId, isEnabled);

            return favoriteStockId;
        }
        else {
            throw FavoriteStockHandler.FORBIDDEN;
        }
    }

    public void deleteFavoriteStock(User user, Long favoriteStockId) {
        FavoriteStock favoriteStock = favoriteStockQueryService.getFavoriteStockById(favoriteStockId);

        if (favoriteStock.getUser().getId().equals(user.getId())) {
            favoriteStockCommandService.deleteFavoriteStock(favoriteStockId);
        }
        else {
            throw FavoriteStockHandler.FORBIDDEN;
        }
    }
}
