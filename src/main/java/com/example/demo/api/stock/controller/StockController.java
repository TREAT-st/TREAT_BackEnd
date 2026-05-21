package com.example.demo.api.stock.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.stock.dto.StockRequestDto.ImportStockRequest;
import com.example.demo.api.stock.dto.StockResponseDto.ImportStockResponse;
import com.example.demo.api.stock.dto.StockResponseDto.StockPriceResponse;
import com.example.demo.api.stock.dto.StockResponseDto.UploadExcelResponse;
import com.example.demo.api.stock.service.StockUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@Tag(name = "[주식] 주식 종목 API")
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockUseCase stockUseCase;

    @Operation(summary = "전일(가장 최근 일자) 시가/종가 조회",
            description = "종목코드로 전날의(가장 최근 날짜) 시가와 종가를 KIS API에서 조회하고 DB에 저장합니다.")
    @GetMapping("/{stockCode}/today-price")
    public ApiResponseDto<StockPriceResponse> getLatestStockPrice(@PathVariable String stockCode) {
        return ApiResponseDto.onSuccess(stockUseCase.getLatestStockPriceByStockCode(stockCode));
    }

    @Operation(summary = "코스피200 엑셀 파일 S3 업로드",
            description = "로컬에서 다운로드한 KRX 코스피200 엑셀 파일을 S3에 업로드합니다.")
    @PostMapping(value = "/excel/upload", consumes = "multipart/form-data")
    public ApiResponseDto<UploadExcelResponse> uploadStockExcel(
            @RequestPart("file") MultipartFile file) {
        return ApiResponseDto.onSuccess(stockUseCase.uploadStockExcel(file));
    }

    @Operation(summary = "S3 엑셀 파일 DB 저장",
            description = "S3에 업로드된 코스피200 엑셀 파일을 읽어 종목 정보를 DB에 저장합니다.")
    @PostMapping("/excel/import")
    public ApiResponseDto<ImportStockResponse> importStockFromS3(
            @RequestBody @Valid ImportStockRequest request) {
        return ApiResponseDto.onSuccess(stockUseCase.importStockFromS3(request.getS3Uri()));
    }
}
