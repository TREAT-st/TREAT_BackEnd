package com.example.demo.api.stock.mapper;

import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.api.stock.dto.StockResponseDto.StockItemResponse;
import com.example.demo.api.stock.dto.StockResponseDto.StockOpenAndClosePriceResponse;
import com.example.demo.api.stock.dto.StockResponseDto.StockPageResponse;
import com.example.demo.api.stock.dto.StockResponseDto.SyncStocksResponse;
import com.example.demo.domain.stock.entity.Kospi200SyncCommand;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.example.demo.domain.stock.entity.StockSyncOutcome;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StockConverter {

    /**
     * KRX 응답을 동기화 입력으로 변환한다.
     * 시세 유무는 errors가 아니라 응답 데이터(hasPrice)가 직접 말해주므로 그 기준으로 가른다.
     */
    public static Kospi200SyncCommand toKospi200SyncCommand(KrxKospi200ResponseDto response) {
        Map<String, String> incomingStocks = response.getStocks().stream()
                .collect(Collectors.toMap(
                        KrxKospi200ResponseDto.StockPrice::getStockCode,
                        KrxKospi200ResponseDto.StockPrice::getStockName));

        List<KrxKospi200ResponseDto.ErrorItem> errors =
                response.getErrors() == null ? List.of() : response.getErrors();
        Set<String> errorCodes = errors.stream()
                .map(KrxKospi200ResponseDto.ErrorItem::getStockCode)
                .collect(Collectors.toSet());

        List<StockPriceSnapshot> priceSnapshots = response.getStocks().stream()
                .filter(KrxKospi200ResponseDto.StockPrice::hasPrice)
                .map(s -> new StockPriceSnapshot(s.getStockCode(), s.getOpenPrice(), s.getClosePrice()))
                .toList();

        List<String> priceUnavailableStockCodes = response.getStocks().stream()
                .filter(s -> !s.hasPrice())
                .map(KrxKospi200ResponseDto.StockPrice::getStockCode)
                .sorted()
                .toList();

        return new Kospi200SyncCommand(
                LocalDate.parse(response.getTradeDate(), DateTimeFormatter.BASIC_ISO_DATE),
                incomingStocks, errorCodes, priceSnapshots, priceUnavailableStockCodes);
    }

    public static SyncStocksResponse toSyncStocksResponse(Kospi200SyncCommand command, StockSyncOutcome outcome) {
        return SyncStocksResponse.builder()
                .tradeDate(command.tradeDate())
                .addedCount(outcome.syncResult().addedCount())
                .updatedCount(outcome.syncResult().updatedCount())
                .deactivatedCount(outcome.syncResult().deactivatedCount())
                .reactivatedCount(outcome.syncResult().reactivatedCount())
                .priceUpdatedCount(outcome.priceUpdatedCount())
                .unresolvedStockCodes(outcome.syncResult().unresolvedStockCodes())
                .priceUnavailableStockCodes(command.priceUnavailableStockCodes())
                .priceUpdateSkippedStockCodes(outcome.priceUpdateSkippedStockCodes())
                .build();
    }

    public static StockOpenAndClosePriceResponse toStockOpenAndClosePriceResponse(Stock stock) {
        return StockOpenAndClosePriceResponse.builder()
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .openPrice(stock.getOpenPrice())
                .closePrice(stock.getClosePrice())
                .tradeDate(stock.getTradeDate())
                .isActive(stock.getIsActive())
                .build();
    }

    public static StockItemResponse toStockItemResponse(Stock stock) {
        return StockItemResponse.builder()
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .isActive(stock.getIsActive())
                .build();
    }

    public static List<StockItemResponse> toStockItemResponseList(List<Stock> stocks) {
        return stocks.stream()
                .map(StockConverter::toStockItemResponse)
                .collect(Collectors.toList());
    }

    public static StockPageResponse toStockPageResponse(Page<Stock> page) {
        List<StockItemResponse> content = toStockItemResponseList(page.getContent());
        return StockPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
