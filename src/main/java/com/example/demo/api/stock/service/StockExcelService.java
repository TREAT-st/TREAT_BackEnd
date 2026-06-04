package com.example.demo.api.stock.service;

import com.example.demo.api.stock.dto.StockSyncResultDto;
import com.example.demo.common.service.S3Service;
import com.example.demo.common.util.ExcelUtil;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.service.StockCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockExcelService {

    private final StockCommandService stockCommandService;
    private final S3Service s3Service;
    private final ExcelUtil excelUtil;

    public StockSyncResultDto syncKOSPI200FromExcel(String s3Uri) {
        Map<String, String> excelStocks = parseExcelFromS3(s3Uri);
        return stockCommandService.syncStocks(excelStocks);
    }

    private Map<String, String> parseExcelFromS3(String s3Uri) {
        Map<String, String> excelStocks = new LinkedHashMap<>();

        try (InputStream inputStream = s3Service.downloadFile(s3Uri);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String stockCode = excelUtil.getCellStringValue(row.getCell(0));
                String stockName = excelUtil.getCellStringValue(row.getCell(1));

                if (stockCode.isEmpty() || stockName.isEmpty()) continue;

                if (excelStocks.containsKey(stockCode)) {
                    log.info("파일 내 중복 종목 스킵: {}", stockCode);
                    continue;
                }

                excelStocks.put(stockCode, stockName);
            }

        } catch (SdkException e) {
            log.error("S3 다운로드 실패. uri={}", s3Uri, e);
            throw StockHandler.s3FileIoError();
        } catch (IOException | IllegalArgumentException e) {
            log.error("유효하지 않은 엑셀 파일. uri={}", s3Uri, e);
            throw StockHandler.invalidFile();
        }

        if (excelStocks.isEmpty()) {
            throw StockHandler.emptyStock();
        }

        return excelStocks;
    }
}
