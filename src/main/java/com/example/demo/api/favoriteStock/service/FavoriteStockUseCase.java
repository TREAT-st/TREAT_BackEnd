package com.example.demo.api.favoriteStock.service;

import com.example.demo.api.favoriteStock.dto.FavoriteStockRequestDto;
import com.example.demo.api.favoriteStock.mapper.FavoriteStockConverter;
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
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FavoriteStockUseCase {
    private final FavoriteStockQueryService favoriteStockQueryService;
    private final FavoriteStockCommandService favoriteStockCommandService;

    @Transactional
    public Long addFavoriteStock(User user, FavoriteStockRequestDto request) {
        FavoriteStock favoriteStock = FavoriteStockConverter.toFavoriteStock(user, request);

        return favoriteStockCommandService.addFavoriteStock(favoriteStock);
    }

    public Page<FavoriteStock> getFavoriteStockPageByUserId(Long userId, Pageable pageable) {
        return favoriteStockQueryService.getUserFavoriteStockListByPage(userId, pageable);
    }

    @Transactional
    public void deleteFavoriteStock(Long userId, Long favoriteStockId) {
        FavoriteStock favoriteStock = favoriteStockQueryService.getFavoriteStockById(favoriteStockId);

        if (!favoriteStock.getUser().getId().equals(userId)) {
            throw FavoriteStockHandler.FORBIDDEN;
        }

        favoriteStockCommandService.deleteFavoriteStock(favoriteStockId);
    }
}
