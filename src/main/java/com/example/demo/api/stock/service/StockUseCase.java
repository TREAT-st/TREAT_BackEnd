package com.example.demo.api.stock.service;

import com.example.demo.api.kis.dto.KisResponseDto;
import com.example.demo.api.kis.service.KisService;
import com.example.demo.api.stock.dto.StockResponseDto.ImportStockResponse;
import com.example.demo.api.stock.dto.StockResponseDto.StockPriceResponse;
import com.example.demo.api.stock.dto.StockResponseDto.UploadExcelResponse;
import com.example.demo.api.stock.mapper.StockConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.common.service.S3Service;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.service.StockCommandService;
import com.example.demo.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@UseCase
@RequiredArgsConstructor
public class StockUseCase {

    private final StockCommandService stockCommandService;
    private final StockQueryService stockQueryService;
    private final KisService kisService;
    private final S3Service s3Service;
    private final StockExcelService stockExcelService;

    @Value("${cloud.aws.s3.bucket.kospi200}")
    private String bucketName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional
    public StockPriceResponse getLatestStockPriceByStockCode(String stockCode) {
        for (int i = 1; i <= 10; i++) {
            LocalDate targetDate = LocalDate.now().minusDays(i);

            if (isWeekend(targetDate)) continue;

            String dateStr = targetDate.format(DATE_FORMATTER);
            KisResponseDto response = kisService.getDailyStockPrice(stockCode, dateStr, dateStr);

            List<KisResponseDto.DailyData> output2 = response.getOutput2();
            if (output2 == null || output2.isEmpty()) continue;

            String rawOpen  = output2.get(0).getOpenPrice();
            String rawClose = output2.get(0).getClosePrice();
            if (rawOpen == null || rawOpen.isBlank() || rawClose == null || rawClose.isBlank()) continue;

            BigDecimal openPrice  = new BigDecimal(rawOpen);
            BigDecimal closePrice = new BigDecimal(rawClose);

            Stock stock = stockCommandService.updateStockPrice(stockCode, openPrice, closePrice, targetDate);
            return StockConverter.toStockPriceResponse(stock);
        }

        throw StockHandler.stockPriceResponseEmpty();
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    @Transactional
    public UploadExcelResponse uploadStockExcel(MultipartFile file) {
        validateExcelFile(file);
        try {
            String s3Uri = "s3://" + bucketName + "/" + file.getOriginalFilename();
            String uploadedUri = s3Service.uploadFile(file, s3Uri);
            return UploadExcelResponse.builder()
                    .s3Uri(uploadedUri)
                    .build();
        } catch (IOException e) {
            throw StockHandler.s3FileIoError();
        }
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw StockHandler.invalidFile();
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw StockHandler.invalidFile();
        }
    }

    @Transactional
    public ImportStockResponse importStockFromS3(String s3Uri) {
        try {
            int savedCount = stockExcelService.importFromExcel(s3Uri);
            return ImportStockResponse.builder()
                    .savedCount(savedCount)
                    .build();
        } catch (IOException e) {
            throw StockHandler.s3FileIoError();
        }
    }

    @Transactional(readOnly = true)
    public Page<Stock> getAllStocks(Pageable pageable) {
        return stockQueryService.getAllStocks(pageable);
    }
}
