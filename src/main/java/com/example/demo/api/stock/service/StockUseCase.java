package com.example.demo.api.stock.service;

import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.api.stock.dto.StockResponseDto.*;
import com.example.demo.api.stock.dto.StockSyncResultDto;
import com.example.demo.api.stock.mapper.StockConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.example.demo.domain.stock.service.StockCommandService;
import com.example.demo.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class StockUseCase {

    private final StockCommandService stockCommandService;
    private final StockQueryService stockQueryService;
    private final KrxService krxService;

    @Transactional
    public SyncStocksResponse syncKospi200FromKrx() {
        KrxKospi200ResponseDto response = krxService.getKospi200Prices();
        LocalDate tradeDate = LocalDate.parse(response.getTradeDate(), DateTimeFormatter.BASIC_ISO_DATE);

        Map<String, String> codeToName = response.getStocks().stream()
                .collect(Collectors.toMap(
                        KrxKospi200ResponseDto.StockPrice::getStockCode,
                        KrxKospi200ResponseDto.StockPrice::getStockName));

        StockSyncResultDto syncResult = stockCommandService.syncStocks(codeToName);

        List<StockPriceSnapshot> snapshots = response.getStocks().stream()
                .map(s -> new StockPriceSnapshot(s.getStockCode(), s.getOpenPrice(), s.getClosePrice()))
                .toList();
        int priceUpdatedCount = stockCommandService.updateStockPrices(snapshots, tradeDate);

        List<String> excluded = response.getErrors() == null ? List.of()
                : response.getErrors().stream()
                        .map(KrxKospi200ResponseDto.ErrorItem::getStockCode)
                        .toList();

        log.info("코스피200 동기화 완료. tradeDate={} 신규={} 이름변경={} 편출={} 재편입={} 시세반영={}",
                tradeDate, syncResult.addedCount(), syncResult.updatedCount(),
                syncResult.deactivatedCount(), syncResult.reactivatedCount(), priceUpdatedCount);

        return SyncStocksResponse.builder()
                .tradeDate(tradeDate)
                .addedCount(syncResult.addedCount())
                .updatedCount(syncResult.updatedCount())
                .deactivatedCount(syncResult.deactivatedCount())
                .reactivatedCount(syncResult.reactivatedCount())
                .priceUpdatedCount(priceUpdatedCount)
                .excludedStockCodes(excluded)
                .build();
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
