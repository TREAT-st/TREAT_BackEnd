package com.example.demo.api.stock.service;

import com.example.demo.api.kis.dto.KisResponseDto;
import com.example.demo.api.kis.service.KisService;
import com.example.demo.api.stock.dto.StockResponseDto.*;
import com.example.demo.api.stock.dto.StockSyncResultDto;
import com.example.demo.api.stock.mapper.StockConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.common.service.S3Service;
import com.example.demo.common.util.DateUtil;
import com.example.demo.common.util.ExcelUtil;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.service.StockCommandService;
import com.example.demo.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.example.demo.common.consts.StaticVariable.DATE_FORMATTER;
import static com.example.demo.common.consts.StaticVariable.KOSPI200_FILE_KEY;

@UseCase
@RequiredArgsConstructor
public class StockUseCase {

    private final StockCommandService stockCommandService;
    private final StockQueryService stockQueryService;
    private final KisService kisService;
    private final S3Service s3Service;
    private final StockExcelService stockExcelService;
    private final DateUtil dateUtil;
    private final ExcelUtil excelUtil;

    @Value("${cloud.aws.s3.bucket.kospi200}")
    private String bucketName;

    public UploadExcelResponse uploadStockExcel(MultipartFile file) {
        excelUtil.validateExcelFile(file);

        String s3Uri = "s3://" + bucketName + "/" + KOSPI200_FILE_KEY;

        try {
            s3Service.uploadFile(file, s3Uri);
            return UploadExcelResponse.builder()
                    .s3Uri(s3Uri)
                    .build();
        } catch (IOException | SdkException e) {
            throw StockHandler.s3FileIoError();
        }
    }

    public ImportStockResponse importStockFromS3() {
        String s3Uri = "s3://" + bucketName + "/" + KOSPI200_FILE_KEY;
        StockSyncResultDto result = stockExcelService.syncKOSPI200FromExcel(s3Uri);

        return ImportStockResponse.builder()
                .savedCount(result.addedCount())
                .updatedCount(result.updatedCount())
                .deletedCount(result.deletedCount())
                .build();
    }

    public StockPriceResponse getLatestStockPriceByStockCode(String stockCode) {
        for (int i = 1; i <= 10; i++) {
            LocalDate targetDate = LocalDate.now().minusDays(i);

            if (dateUtil.isWeekend(targetDate)) continue;

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

    public Page<Stock> getAllStocks(Pageable pageable) {
        return stockQueryService.getAllStocks(pageable);
    }
}
