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

        favoriteStockRepository.save(favoriteStock);

        return favoriteStock.getId();
    }

    @Override
    public boolean turnOnFavoriteStockAlarm(Long favoriteStockId) {
        FavoriteStock favoriteStock = favoriteStockRepository.findById(favoriteStockId)
                .orElseThrow(() -> FavoriteStockHandler.NOT_FOUND);

        if (!favoriteStock.getIsAlertEnabled()) {
            favoriteStock.setIsAlertEnabled(true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean turnOffFavoriteStockAlarm(Long favoriteStockId) {
        FavoriteStock favoriteStock = favoriteStockRepository.findById(favoriteStockId)
                .orElseThrow(() -> FavoriteStockHandler.NOT_FOUND);

        if (favoriteStock.getIsAlertEnabled()) {
            favoriteStock.setIsAlertEnabled(false);
            return true;
        }
        else {
            return false;
        }
    }

    //  TODO: hard
    @Override
    public Void deleteFavoriteStock(Long favoriteStockId) {
        favoriteStockRepository.deleteById(favoriteStockId);
        return null;
    }
}
