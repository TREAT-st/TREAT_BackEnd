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

    /**
     * KRX 조회는 200종목 크롤링이라 수 분이 걸릴 수 있다. 트랜잭션 안에서 호출하면 그동안
     * DB 커넥션을 붙잡게 되므로, 조회·변환은 트랜잭션 밖에서 끝내고 DB 쓰기만 커맨드에 맡긴다.
     */
    public SyncStocksResponse syncKospi200FromKrx() {
        return syncKospi200(fetchKospi200());
    }

    /**
     * 조회만 한다. 배치는 거래일을 먼저 확인하고 실행 이력을 연 뒤에 DB를 써야 해서
     * 조회와 반영 사이에 끼어들 지점이 필요하다.
     */
    public Kospi200SyncCommand fetchKospi200() {
        KrxKospi200ResponseDto response = krxService.getKospi200Prices();
        return StockConverter.toKospi200SyncCommand(response);
    }

    /** 이미 받아둔 KRX 응답을 DB에 반영한다. */
    public SyncStocksResponse syncKospi200(Kospi200SyncCommand command) {
        StockSyncOutcome outcome = stockCommandService.syncStocksAndPrices(command);
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
}
