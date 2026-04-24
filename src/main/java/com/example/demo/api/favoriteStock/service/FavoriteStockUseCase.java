package com.example.demo.api.favoriteStock.service;

import com.example.demo.api.favoriteStock.dto.FavoriteStockResponseDto.*;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.favoriteStock.service.FavoriteStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@UseCase
@RequiredArgsConstructor
public class FavoriteStockUseCase {
    private final FavoriteStockQueryService favoriteStockQueryService;

    public Page<FavoriteStock> getFavoriteStockPageByUserId(Long userId, Pageable pageable) {
        return favoriteStockQueryService.getUserFavoriteStockListByPage(userId, pageable);
    }
}
