package com.example.demo.domain.favoriteStock.service;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.favoriteStock.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteCommandServiceImpl implements FavoriteCommandService {
    private final FavoriteRepository favoriteRepository;

    // TODO: builder로 처리하기
    @Override
    public FavoriteStock addFavoriteStock(FavoriteStock favoriteStock) {
        boolean exists = favoriteRepository.existsByUserIdAndStockCode(
                favoriteStock.getUser().getId(), favoriteStock.getStockCode());
        if (exists) {
            throw new IllegalStateException("Already added to favorites");
        }
        return favoriteRepository.save(favoriteStock);
    }

    @Override
    public boolean turnOnFavoriteStockAlarm(Long favoriteStockId) {
        FavoriteStock favoriteStock = favoriteRepository.findById(favoriteStockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관심 종목입니다."));

        if (!favoriteStock.getIsAlertEnabled()) {
            favoriteStock.setIsAlertEnabled(true);
            favoriteRepository.save(favoriteStock);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean turnOffFavoriteStockAlarm(Long favoriteStockId) {
        FavoriteStock favoriteStock = favoriteRepository.findById(favoriteStockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관심 종목입니다."));

        if (favoriteStock.getIsAlertEnabled()) {
            favoriteStock.setIsAlertEnabled(false);
            favoriteRepository.save(favoriteStock);
            return true;
        }
        else {
            return false;
        }
    }

    //  TODO: hard
    @Override
    public Long deleteFavoriteStock(Long favoriteStockId) {
        FavoriteStock favoriteStock = favoriteRepository.findById(favoriteStockId)
                .orElseThrow(() -> new IllegalArgumentException("FavoriteStock not found: " + favoriteStockId));
        favoriteRepository.delete(favoriteStock);
        return favoriteStock.getId();
    }
}
