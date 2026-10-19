package com.example.demo.api.favoriteStock.service;

import com.example.demo.api.favoriteStock.dto.FavoriteStockResponseDto.FavoriteStockDto;
import com.example.demo.api.favoriteStock.dto.FavoriteStockResponseDto.FavoriteStockPageResponse;
import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.favoriteStock.service.FavoriteStockCommandService;
import com.example.demo.domain.favoriteStock.service.FavoriteStockQueryService;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.example.demo.domain.stock.service.StockCommandService;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 관심종목 목록에 시세를 붙이는 조합 검증.
 *
 * 관심종목은 stock을 종목코드 문자열로만 가리키므로(FK 없음), 조합은 UseCase에서만 한다.
 * 페이지의 종목코드로 한 번에 조회해야 하고, stock에 없는 종목이어도 관심종목은 빠지면 안 된다.
 */
class FavoriteStockUseCaseTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 10, 16);
    private static final Pageable PAGE = PageRequest.of(0, 10);

    private final FavoriteStockQueryService favoriteStockQueryService = Mockito.mock(FavoriteStockQueryService.class);
    private final StockQueryService stockQueryService = Mockito.mock(StockQueryService.class);

    private final FavoriteStockUseCase useCase = new FavoriteStockUseCase(
            favoriteStockQueryService,
            Mockito.mock(FavoriteStockCommandService.class),
            Mockito.mock(StockCommandService.class),
            stockQueryService);

    private final User user = User.builder().id(1L).username("tester").build();

    @Test
    void 관심종목마다_최신_시세가_붙는다() {
        givenFavorites(favorite(10L, "005930", "삼성전자"));
        givenStocks(stock("005930", "삼성전자", "72500", "-1200", "-1.63"));

        FavoriteStockDto dto = useCase.getFavoriteStockPageByUserId(1L, PAGE).getContent().get(0);

        assertThat(dto.getCurrentPrice()).isEqualByComparingTo("72500");
        assertThat(dto.getPriceChange()).isEqualByComparingTo("-1200");
        assertThat(dto.getChangeRate()).isEqualByComparingTo("-1.63");
        assertThat(dto.getTradeDate()).isEqualTo(TRADE_DATE);
        // 관심종목 고유 정보는 그대로다.
        assertThat(dto.getFavoriteStockId()).isEqualTo(10L);
        assertThat(dto.getUserId()).isEqualTo(1L);
    }

    /** 종목마다 조회하면 페이지 크기만큼 쿼리가 나간다(N+1). 한 번에 조회해야 한다. */
    @Test
    void 페이지의_종목코드로_한_번만_조회한다() {
        givenFavorites(favorite(10L, "005930", "삼성전자"), favorite(11L, "000660", "SK하이닉스"));
        givenStocks();

        useCase.getFavoriteStockPageByUserId(1L, PAGE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> codes = ArgumentCaptor.forClass(Collection.class);
        verify(stockQueryService, times(1)).getStocksByCodes(codes.capture());
        assertThat(codes.getValue()).containsExactlyInAnyOrder("005930", "000660");
    }

    /** FK가 없어 stock에 없는 코드로 등록된 관심종목이 있을 수 있다. 관심종목은 그대로, 시세만 비운다. */
    @Test
    void stock에_없는_종목도_관심종목은_그대로_나가고_시세만_비어_있다() {
        givenFavorites(favorite(10L, "005930", "삼성전자"), favorite(11L, "999999", "없는종목"));
        givenStocks(stock("005930", "삼성전자", "72500", "-1200", "-1.63"));

        FavoriteStockPageResponse response = useCase.getFavoriteStockPageByUserId(1L, PAGE);

        assertThat(response.getContent()).hasSize(2);
        FavoriteStockDto missing = response.getContent().get(1);
        assertThat(missing.getStockCode()).isEqualTo("999999");
        assertThat(missing.getStockName()).isEqualTo("없는종목");
        assertThat(missing.getCurrentPrice()).isNull();
        assertThat(missing.getPriceChange()).isNull();
        assertThat(missing.getChangeRate()).isNull();
        assertThat(missing.getTradeDate()).isNull();
    }

    /** 관심종목의 종목명은 등록 시점 복사본이다. 종목명이 바뀌었으면 stock 쪽이 최신이다. */
    @Test
    void 종목명은_stock_쪽을_우선한다() {
        givenFavorites(favorite(10L, "005930", "옛이름"));
        givenStocks(stock("005930", "삼성전자", "72500", null, null));

        assertThat(useCase.getFavoriteStockPageByUserId(1L, PAGE).getContent().get(0).getStockName())
                .isEqualTo("삼성전자");
    }

    @Test
    void 페이지_정보는_관심종목_페이지를_따른다() {
        givenFavorites(favorite(10L, "005930", "삼성전자"));
        givenStocks();

        FavoriteStockPageResponse response = useCase.getFavoriteStockPageByUserId(1L, PAGE);

        assertThat(response.getPage()).isZero();
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isHasNext()).isFalse();
    }

    private void givenFavorites(FavoriteStock... favorites) {
        Mockito.when(favoriteStockQueryService.getUserFavoriteStockListByPage(1L, PAGE))
                .thenReturn(new PageImpl<>(List.of(favorites), PAGE, favorites.length));
    }

    private void givenStocks(Stock... stocks) {
        Map<String, Stock> byCode = new java.util.HashMap<>();
        for (Stock stock : stocks) {
            byCode.put(stock.getStockCode(), stock);
        }
        Mockito.when(stockQueryService.getStocksByCodes(any())).thenReturn(byCode);
    }

    private FavoriteStock favorite(Long id, String stockCode, String stockName) {
        return FavoriteStock.builder().id(id).user(user).stockCode(stockCode).stockName(stockName).build();
    }

    private Stock stock(String stockCode, String stockName, String close, String change, String rate) {
        Stock stock = Stock.builder().stockCode(stockCode).stockName(stockName).build();
        stock.updatePrice(new StockPriceSnapshot(stockCode, new BigDecimal(close), new BigDecimal(close), null,
                change == null ? null : new BigDecimal(change),
                rate == null ? null : new BigDecimal(rate)), TRADE_DATE);
        return stock;
    }
}
