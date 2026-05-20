package com.example.demo.api.stock.service;

import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.service.StockCommandService;
import com.example.demo.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockExcelService {

    private final StockQueryService stockQueryService;
    private final StockCommandService stockCommandService;

    public int importFromExcel(String filePath) throws IOException, InvalidFormatException {
        File file = new File(filePath);
        List<Stock> stocks = new ArrayList<>();
        //String fileName = file.getName();
        //String dateStr = fileName.replaceAll(".*_(\\d{8})\\.xlsx", "$1");
        //LocalDate inquiryDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

        Set<String> existingCodes = stockQueryService.getAllStockCodes();
        Set<String> seenInFile = new HashSet<>();

        try (Workbook workbook = new XSSFWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String stockCode = getCellStringValue(row.getCell(0));
                String stockName  = getCellStringValue(row.getCell(1));
                //BigDecimal closePrice = getCellNumericValue(row.getCell(2));

                if (stockCode.isEmpty() || stockName.isEmpty()) continue;
                if (!seenInFile.add(stockCode)) {
                    log.info("파일 내 중복 종목 스킵: {}", stockCode);
                    continue;
                }
                if (existingCodes.contains(stockCode)) {
                    log.info("이미 존재하는 종목 스킵: {}", stockCode);
                    continue;
                }

                Stock stock = Stock.builder()
                        .stockCode(stockCode)
                        .stockName(stockName)
                        .closePrice(null)
                        .openPrice(null)
                        .inquiryDate(null)
                        .build();

                stocks.add(stock);
            }
        }

        stockCommandService.saveAllStocks(stocks);
        log.info("총 {}개 종목 저장 완료", stocks.size());

        return stocks.size();
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.format("%06d", (long) cell.getNumericCellValue());
            default -> "";
        };
    }
}
