package com.example.demo.api.stock.service;

import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.api.stock.dto.StockResponseDto.*;
import com.example.demo.api.stock.dto.StockSyncResultDto;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.common.service.S3Service;
import com.example.demo.common.util.ExcelUtil;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.service.StockCommandService;
import com.example.demo.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.demo.common.consts.StaticVariable.KOSPI200_FILE_KEY;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class StockUseCase {

    private final StockCommandService stockCommandService;
    private final StockQueryService stockQueryService;
    private final KrxService krxService;
    private final S3Service s3Service;
    private final StockExcelService stockExcelService;
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
                .deactivatedCount(result.deactivatedCount())
                .reactivatedCount(result.reactivatedCount())
                .build();
    }

    /**
     * KRX Lambda로 코스피200 구성종목과 시가·종가를 한 번에 받아 반영한다.
     * 목록 동기화를 먼저 끝내야 신규 편입 종목에도 시세를 넣을 수 있으므로 순서를 지킨다.
     */
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
    public Page<Stock> getAllStocks(Pageable pageable) {
        return stockQueryService.getAllActiveStocks(pageable);
    }
}
