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
import java.util.Set;
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

        // stocks에는 구성종목이 전부 들어온다. 시세를 못 받은 종목도 코드·이름은 있으므로
        // 목록 동기화는 전체를 대상으로 하고, 시세 갱신만 가격이 있는 종목으로 좁힌다.
        Map<String, String> codeToName = response.getStocks().stream()
                .collect(Collectors.toMap(
                        KrxKospi200ResponseDto.StockPrice::getStockCode,
                        KrxKospi200ResponseDto.StockPrice::getStockName));

        // errors에는 성격이 다른 둘이 섞여 온다. stocks에 있는지로 구분한다.
        List<KrxKospi200ResponseDto.ErrorItem> errors =
                response.getErrors() == null ? List.of() : response.getErrors();

        // 종목명조차 못 받아 stocks에서 빠진 종목. 편출인지 알 수 없으므로 판정을 보류한다.
        List<String> unresolvedStockCodes = errors.stream()
                .map(KrxKospi200ResponseDto.ErrorItem::getStockCode)
                .filter(code -> !codeToName.containsKey(code))
                .distinct()
                .toList();

        // 목록에는 반영되지만 시세만 못 받은 종목(거래정지 등). 편출이 아니다.
        List<String> priceUnavailableStockCodes = errors.stream()
                .map(KrxKospi200ResponseDto.ErrorItem::getStockCode)
                .filter(codeToName::containsKey)
                .distinct()
                .toList();

        StockSyncResultDto syncResult =
                stockCommandService.syncStocks(codeToName, Set.copyOf(unresolvedStockCodes));

        List<StockPriceSnapshot> snapshots = response.getStocks().stream()
                .filter(KrxKospi200ResponseDto.StockPrice::hasPrice)
                .map(s -> new StockPriceSnapshot(s.getStockCode(), s.getOpenPrice(), s.getClosePrice()))
                .toList();
        int priceUpdatedCount = stockCommandService.updateStockPrices(snapshots, tradeDate);

        log.info("코스피200 동기화 완료. tradeDate={} 신규={} 이름변경={} 편출={} 재편입={} 시세반영={} 시세미수신={} 미해결={}",
                tradeDate, syncResult.addedCount(), syncResult.updatedCount(),
                syncResult.deactivatedCount(), syncResult.reactivatedCount(), priceUpdatedCount,
                priceUnavailableStockCodes.size(), unresolvedStockCodes.size());

        return SyncStocksResponse.builder()
                .tradeDate(tradeDate)
                .addedCount(syncResult.addedCount())
                .updatedCount(syncResult.updatedCount())
                .deactivatedCount(syncResult.deactivatedCount())
                .reactivatedCount(syncResult.reactivatedCount())
                .priceUpdatedCount(priceUpdatedCount)
                .unresolvedStockCodes(unresolvedStockCodes)
                .priceUnavailableStockCodes(priceUnavailableStockCodes)
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
