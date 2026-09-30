package com.example.demo.api.favoriteStock.service;

import com.example.demo.api.favoriteStock.dto.FavoriteStockRequestDto;
import com.example.demo.api.favoriteStock.dto.FavoriteStockResponseDto.DistinctStockResponse;
import com.example.demo.api.favoriteStock.mapper.FavoriteStockConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.favoriteStock.exception.FavoriteStockHandler;
import com.example.demo.domain.favoriteStock.service.FavoriteStockCommandService;
import com.example.demo.domain.favoriteStock.service.FavoriteStockQueryService;
import com.example.demo.domain.stock.service.StockCommandService;
import com.example.demo.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@Transactional
@RequiredArgsConstructor
public class FavoriteStockUseCase {
    private final FavoriteStockQueryService favoriteStockQueryService;
    private final FavoriteStockCommandService favoriteStockCommandService;
    private final StockCommandService stockCommandService;

    /**
     * 관심종목 등록과 관심등록수 증가를 한 트랜잭션으로 묶는다.
     * 둘이 갈라지면 등록은 됐는데 카운트는 안 오른 상태가 남아 집계가 영구히 어긋난다.
     *
     * 중복 등록은 addFavoriteStock이 먼저 막으므로(userId+stockCode 유니크) 카운트가 부풀지 않는다.
     */
    public Long addFavoriteStock(User user, FavoriteStockRequestDto request) {
        FavoriteStock favoriteStock = FavoriteStockConverter.toFavoriteStock(user, request);

        Long favoriteStockId = favoriteStockCommandService.addFavoriteStock(favoriteStock);
        stockCommandService.increaseLikeCount(favoriteStock.getStockCode());

        return favoriteStockId;
    }

    @Transactional(readOnly = true)
    public Page<FavoriteStock> getFavoriteStockPageByUserId(Long userId, Pageable pageable) {
        return favoriteStockQueryService.getUserFavoriteStockListByPage(userId, pageable);
    }

    /**
     * 해제할 때도 카운트를 내려야 한다. 등록만 세면 해제한 사용자가 계속 포함돼
     * likeCount가 실제 관심등록수보다 커지고, 시간이 갈수록 벌어진다.
     */
    public void deleteFavoriteStock(Long userId, Long favoriteStockId) {
        FavoriteStock favoriteStock = favoriteStockQueryService.getFavoriteStockById(favoriteStockId);

        if (!favoriteStock.getUser().getId().equals(userId)) {
            throw FavoriteStockHandler.FORBIDDEN;
        }

        favoriteStockCommandService.deleteFavoriteStock(favoriteStockId);
        stockCommandService.decreaseLikeCount(favoriteStock.getStockCode());
    }
}
