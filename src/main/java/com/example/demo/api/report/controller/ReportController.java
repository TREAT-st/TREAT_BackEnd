package com.example.demo.api.report.controller;

import com.example.demo.infra.S3.dto.S3Response;
import com.example.demo.infra.S3.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자 리포트 페이지] 사용자 리포트 API")
@RestController
@RequestMapping("/api/v1/users/reports")
@RequiredArgsConstructor
public class ReportController {
    private final S3Service S3Service;

    @Operation(summary = "리포트 조회", description = "종목 코드와 날짜로 S3에 저장된 리포트 URL을 조회합니다.")
    @GetMapping
    public ResponseEntity<S3Response> getReport(
            @Parameter(description = "종목 코드 (예: 005930)") @RequestParam String stockCode,
            @Parameter(description = "날짜 (예: 20240101)") @RequestParam String date
    ) {

        String url =
                S3Service.getReportUrl(stockCode, date);

        return ResponseEntity.ok(
                new S3Response(
                        stockCode,
                        date,
                        url
                )
        );
    }
}
