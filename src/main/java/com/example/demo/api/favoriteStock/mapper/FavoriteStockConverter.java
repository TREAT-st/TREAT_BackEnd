package com.example.demo.api.favoriteStock.mapper;

import com.example.demo.api.favoriteStock.dto.FavoriteStockRequestDto;
import com.example.demo.api.favoriteStock.dto.FavoriteStockResponseDto.*;
import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class FavoriteStockConverter {
    public static FavoriteStock toFavoriteStock(User user, FavoriteStockRequestDto request) {
        return FavoriteStock.builder()
                .user(user)
                .stockCode(request.getStockCode())
                .ticker(request.getTicker())
                .build();
    }

    public static FavoriteStockDto toFavoriteStockDto(FavoriteStock favoriteStock) {
        return FavoriteStockDto.builder()
                .favoriteStockId(favoriteStock.getId())
                .userId(favoriteStock.getUser().getId())
                .stockCode(favoriteStock.getStockCode())
                .ticker(favoriteStock.getTicker())
                .isAlertEnabled(favoriteStock.getIsAlertEnabled())
                .build();
    }

    public static FavoriteStockSimplePageResponse toFavoriteStockPageDto(Page<FavoriteStock> favoriteStockPage) {
        List<FavoriteStockDto> content = favoriteStockPage.getContent().stream()
                .map(FavoriteStockConverter::toFavoriteStockDto)
                .collect(Collectors.toList());

        return FavoriteStockSimplePageResponse.builder()
                .content(content)
                .page(favoriteStockPage.getNumber())
                .totalPages(favoriteStockPage.getTotalPages())
                .totalElements(favoriteStockPage.getTotalElements())
                .hasNext(favoriteStockPage.hasNext())
                .build();
    }
}
