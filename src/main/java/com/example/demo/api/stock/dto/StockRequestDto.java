package com.example.demo.api.stock.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StockRequestDto {

    @Getter
    @NoArgsConstructor
    public static class ImportStockRequest {
        @NotBlank(message = "S3 URI는 필수입니다.")
        private String s3Uri;
    }
}
