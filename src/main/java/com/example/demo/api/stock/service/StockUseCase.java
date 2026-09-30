package com.example.demo.api.stock.service;

import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.api.stock.dto.StockResponseDto.StockOpenAndClosePriceResponse;
import com.example.demo.api.stock.dto.StockResponseDto.SyncStocksResponse;
import com.example.demo.api.stock.mapper.StockConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.stock.entity.Kospi200SyncCommand;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockSyncOutcome;
import com.example.demo.domain.stock.service.StockCommandService;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.favoriteStock.service.FavoriteStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class StockUseCase {

    private final StockCommandService stockCommandService;
    private final StockQueryService stockQueryService;
    private final KrxService krxService;
    private final FavoriteStockQueryService favoriteStockQueryService;

    /**
     * KRX 조회는 200종목 크롤링이라 수 분이 걸릴 수 있다. 트랜잭션 안에서 호출하면 그동안
     * DB 커넥션을 붙잡게 되므로, 조회·변환은 트랜잭션 밖에서 끝내고 DB 쓰기만 커맨드에 맡긴다.
     */
    public SyncStocksResponse syncKospi200FromKrx(boolean force) {
        KrxKospi200ResponseDto response = krxService.getKospi200Prices();
        Kospi200SyncCommand command = StockConverter.toKospi200SyncCommand(response);
        // 거래일 검증은 커맨드 쪽 트랜잭션 진입부에서 한다. 여기서 먼저 읽으면
        // 조회와 반영이 다른 트랜잭션으로 갈라져 동시 요청 창이 넓어진다.
        StockSyncOutcome outcome = stockCommandService.syncStocksAndPrices(command, force);

        return StockConverter.toSyncStocksResponse(command, outcome);
    }

    @Transactional(readOnly = true)
    public StockOpenAndClosePriceResponse getStockOpenAndClosePrice(String stockCode) {
        Stock stock = stockQueryService.getStockByCode(stockCode);
        return StockConverter.toStockOpenAndClosePriceResponse(stock);
    }

    @Transactional(readOnly = true)
    public Page<Stock> getAllStocks(Boolean isActive, Pageable pageable) {
        return stockQueryService.getAllStocks(isActive, pageable);
    }

    @Transactional(readOnly = true)
    public com.example.demo.api.stock.dto.StockResponseDto.StockPageResponse getAllStocks(
            Long userId, Boolean isActive, Pageable pageable) {
        Page<Stock> stocks = stockQueryService.getAllStocks(isActive, pageable);
        return StockConverter.toStockPageResponse(
                stocks,
                favoriteStockQueryService.getUserFavoriteStockCodes(userId)
        );
    }
}
