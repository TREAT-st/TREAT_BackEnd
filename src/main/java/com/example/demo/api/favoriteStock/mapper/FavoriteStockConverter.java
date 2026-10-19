package com.example.demo.api.favoriteStock.mapper;

import com.example.demo.api.favoriteStock.dto.FavoriteStockRequestDto;
import com.example.demo.api.favoriteStock.dto.FavoriteStockResponseDto.*;
import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public class FavoriteStockConverter {
    public static FavoriteStock toFavoriteStock(User user, FavoriteStockRequestDto request) {
        return FavoriteStock.builder()
                .user(user)
                .stockCode(request.getStockCode())
                .stockName(request.getStockName())
                .isAlertEnabled(false)
                .build();
    }

    /**
     * 관심종목에 그 종목의 최신 시세를 붙인다.
     *
     * 관심종목은 stock을 종목코드 문자열로만 가리켜(FK 없음) stock에 없는 코드일 수 있다.
     * 그때는 관심종목은 그대로 내보내고 시세만 비워둔다.
     *
     * 종목명은 stock 쪽을 우선한다. 관심종목의 종목명은 등록 시점에 복사한 값이라
     * 종목명이 바뀌면 낡은 이름이 남는다.
     *
     * @param stock 없으면 null
     */
    public static FavoriteStockDto toFavoriteStockDto(FavoriteStock favoriteStock, Stock stock) {
        FavoriteStockDto.FavoriteStockDtoBuilder builder = FavoriteStockDto.builder()
                .favoriteStockId(favoriteStock.getId())
                .userId(favoriteStock.getUser().getId())
                .stockCode(favoriteStock.getStockCode())
                .stockName(favoriteStock.getStockName())
                .isAlertEnabled(favoriteStock.getIsAlertEnabled());

        if (stock != null) {
            builder.stockName(stock.getStockName())
                    .currentPrice(stock.getClosePrice())
                    .priceChange(stock.getPriceChange())
                    .changeRate(stock.getChangeRate())
                    .tradeDate(stock.getTradeDate());
        }
        return builder.build();
    }

    /** @param stocksByCode 이 페이지의 종목코드로 한 번에 조회한 종목들 */
    public static FavoriteStockPageResponse toFavoriteStockPageDto(Page<FavoriteStock> favoriteStockPage,
                                                                   Map<String, Stock> stocksByCode) {
        List<FavoriteStockDto> content = favoriteStockPage.getContent().stream()
                .map(favorite -> toFavoriteStockDto(favorite, stocksByCode.get(favorite.getStockCode())))
                .toList();

        return FavoriteStockPageResponse.builder()
                .content(content)
                .page(favoriteStockPage.getNumber())
                .totalPages(favoriteStockPage.getTotalPages())
                .hasNext(favoriteStockPage.hasNext())
                .build();
    }
}
