package com.example.demo.domain.favoriteStock.service;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.favoriteStock.exception.FavoriteStockHandler;
import com.example.demo.domain.favoriteStock.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FavoriteQueryServiceImpl implements FavoriteQueryService {
    private final FavoriteRepository favoriteRepository;

    @Override
    public Page<FavoriteStock> getUserFavoriteStockListByPage(Long userId, Pageable pageable) {
        return favoriteRepository.findAllByUserId(userId, pageable);
    }

    @Override
    public FavoriteStock getUserFavoriteStockByUserIdAndStockCode(Long userId, String stockCode) {
        return favoriteRepository.findByUserIdAndStockCode(userId, stockCode)
                .orElseThrow(() -> FavoriteStockHandler.NOT_FOUND);
    }

    @Override
    public boolean existsFavoriteStockByUserIdAndStockCode(Long userId, String stockCode) {
        return favoriteRepository.existsByUserIdAndStockCode(userId, stockCode);
    }
}
