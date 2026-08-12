package com.example.demo.api.stock.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.stock.dto.StockResponseDto.ImportStockResponse;
import com.example.demo.api.stock.dto.StockResponseDto.StockPageResponse;
import com.example.demo.api.stock.dto.StockResponseDto.SyncStocksResponse;
import com.example.demo.api.stock.dto.StockResponseDto.UploadExcelResponse;
import com.example.demo.api.stock.mapper.StockConverter;
import com.example.demo.api.stock.service.StockUseCase;
import com.example.demo.domain.stock.entity.Stock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Tag(name = "[주식] 주식 종목 API.")
@Validated
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockUseCase stockUseCase;

    @Operation(summary = "코스피200 엑셀 파일 S3 업로드",
            description = "로컬에서 다운로드한 KRX 코스피200 엑셀 파일을 S3에 업로드합니다.")
    @PostMapping(value = "/excel/upload", consumes = "multipart/form-data")
    public ApiResponseDto<UploadExcelResponse> uploadStockExcel(@RequestPart("file") MultipartFile file) {
        return ApiResponseDto.onSuccess(stockUseCase.uploadStockExcel(file));
    }

    @Operation(summary = "S3 엑셀 파일 DB 저장",
            description = "S3에 업로드된 코스피200 엑셀 파일을 읽어 종목 정보를 DB에 저장합니다.")
    @PostMapping("/excel/import")
    public ApiResponseDto<ImportStockResponse> importStockFromS3() {
        return ApiResponseDto.onSuccess(stockUseCase.importStockFromS3());
    }

    @Operation(summary = "코스피200 종목·시세 동기화 (KRX)",
            description = "KRX Lambda로 코스피200 구성종목과 가장 가까운 거래일의 시가/종가를 한 번에 받아 반영합니다.<br>" +
                    "편출된 종목은 삭제하지 않고 비활성 처리하며, 재편입되면 다시 활성으로 되돌립니다.")
    @PostMapping("/sync")
    public ApiResponseDto<SyncStocksResponse> syncKospi200FromKrx() {
        return ApiResponseDto.onSuccess(stockUseCase.syncKospi200FromKrx());
    }

    @Operation(summary = "모든 종목 조회", description = "Stock에 저장된 모든 종목의 코드와 이름을 페이지 단위로 조회합니다.")
    @GetMapping
    public ApiResponseDto<StockPageResponse> getAllStocks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Order.asc("stockCode")));
        Page<Stock> stockPage = stockUseCase.getAllStocks(pageable);
        return ApiResponseDto.onSuccess(StockConverter.toStockPageResponse(stockPage));
    }
}
