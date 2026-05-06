package com.example.demo.domain.favoriteStock.service;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.favoriteStock.exception.FavoriteStockHandler;
import com.example.demo.domain.favoriteStock.repository.FavoriteStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteStockCommandServiceImpl implements FavoriteStockCommandService {
    private final FavoriteStockRepository favoriteStockRepository;

    @Override
    public Long addFavoriteStock(FavoriteStock favoriteStock) {
        if (favoriteStockRepository.existsByUserIdAndStockCode(
                favoriteStock.getUser().getId(), favoriteStock.getStockCode())) {
            throw FavoriteStockHandler.ALREADY_EXISTS;
        }

        return favoriteStockRepository.save(favoriteStock).getId();
    }

    @Override
    public void updateFavoriteStockAlarm(Long favoriteStockId, boolean isEnabled) {
        FavoriteStock favoriteStock = favoriteStockRepository.findById(favoriteStockId)
                .orElseThrow(() -> FavoriteStockHandler.NOT_FOUND);
        favoriteStock.updateAlertEnabled(isEnabled);
    }

    //  TODO: hard
    @Override
    public void deleteFavoriteStock(Long favoriteStockId) {
        favoriteStockRepository.deleteById(favoriteStockId);
    }
}
