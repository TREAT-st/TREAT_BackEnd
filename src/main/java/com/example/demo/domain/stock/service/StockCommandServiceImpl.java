package com.example.demo.domain.stock.service;

import com.example.demo.api.stock.dto.StockSyncResultDto;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StockCommandServiceImpl implements StockCommandService {

    private final StockRepository stockRepository;

    @Override
    public Stock updateStockPrice(
            String stockCode,
            BigDecimal openPrice,
            BigDecimal closePrice,
            LocalDate inquiryDate) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(StockHandler::notFound);
        stock.updatePrice(openPrice, closePrice, inquiryDate);

        return stock;
    }

    @Override
    public StockSyncResultDto syncStocks(Map<String, String> excelStocks) {
        Map<String, String> dbStocks = stockRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Stock::getStockCode,
                        Stock::getStockName
                ));

        Set<String> excelCodes = excelStocks.keySet();
        Set<String> dbCodes = dbStocks.keySet();

        Set<String> toAdd = new HashSet<>(excelCodes);
        toAdd.removeAll(dbCodes);

        Set<String> toDelete = new HashSet<>(dbCodes);
        toDelete.removeAll(excelCodes);

        Map<String, String> toUpdate = excelCodes.stream()
                .filter(dbCodes::contains)
                .filter(code -> !excelStocks.get(code).equals(dbStocks.get(code)))
                .collect(Collectors.toMap(code -> code, excelStocks::get));

        List<Stock> newStocks = toAdd.stream()
                .map(code -> Stock.builder()
                        .stockCode(code)
                        .stockName(excelStocks.get(code))
                        .build())
                .collect(Collectors.toList());

        saveAllStocks(newStocks);
        updateStockNames(toUpdate);
        deleteByStockCodes(toDelete);

        return new StockSyncResultDto(
                toAdd.size(),
                toUpdate.size(),
                toDelete.size()
        );
    }

    @Override
    public void saveAllStocks(List<Stock> stocks) {
        if (stocks.isEmpty()) return;
        stockRepository.saveAll(stocks);
    }

    @Override
    public void updateStockNames(Map<String, String> codeToName) {
        if (codeToName.isEmpty()) return;
        List<Stock> stocks = stockRepository.findAllByStockCodeIn(codeToName.keySet());
        stocks.forEach(stock ->
                stock.updateName(codeToName.get(stock.getStockCode()))
        );
    }

    @Override
    public void deleteByStockCodes(Set<String> stockCodes) {
        if (stockCodes.isEmpty()) return;
        stockRepository.deleteAllByStockCodeIn(stockCodes);
    }
}
