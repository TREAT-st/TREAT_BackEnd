package com.example.demo.api.volatility.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class VolatilityRequestDto {

    @Getter
    @NoArgsConstructor
    public static class ReportGenerationRequest {
        @Schema(description = "사용할 GPT 모델. 비우면 Lambda 기본값을 사용합니다.", example = "gpt-5.6-terra")
        private String gptModel;
    }

    /** 외부(Lambda)에서 들어오는 요청이므로 모든 필드를 검증한다. */
    @Getter
    @NoArgsConstructor
    public static class ReportCallback {
        @Schema(description = "종목 코드(숫자 6자리)", example = "005930")
        @NotBlank(message = "종목 코드는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "종목 코드는 숫자 6자리여야 합니다.")
        private String stockCode;

        @Schema(description = "리포트 생성일(S3 파일명의 날짜, yyyyMMdd)", example = "20260802")
        @NotBlank(message = "리포트 생성일은 필수입니다.")
        @Pattern(regexp = "\\d{8}", message = "리포트 생성일은 yyyyMMdd 형식이어야 합니다.")
        private String reportDate;

        @Schema(description = "S3에 저장된 리포트 URL",
                example = "https://treat-finance-reports.s3.ap-northeast-2.amazonaws.com/005930_삼성전자_20260802_analysis.html")
        @NotBlank(message = "리포트 URL은 필수입니다.")
        @Size(max = 2048, message = "리포트 URL이 너무 깁니다.")
        @Pattern(regexp = "https://.+", message = "리포트 URL은 https로 시작해야 합니다.")
        private String reportUrl;
    }

    @Getter
    @NoArgsConstructor
    public static class SingleReportRequest {
        @Schema(description = "종목 코드(숫자 6자리)", example = "005930")
        @NotBlank(message = "종목 코드는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "종목 코드는 숫자 6자리여야 합니다.")
        private String stockCode;

        @Schema(description = "종목 이름", example = "삼성전자")
        @NotBlank(message = "종목 이름은 필수입니다.")
        private String stockName;

        @Schema(description = "사용할 GPT 모델. 비우면 Lambda 기본값을 사용합니다.", example = "gpt-5.6-terra")
        private String gptModel;
    }
}
