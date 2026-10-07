package com.example.demo.api.prediction.dto;

import com.example.demo.domain.prediction.entity.PredictionDuration;
import com.example.demo.domain.prediction.entity.PredictionTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class PredictionRequestDto {

    @Getter
    public static class SubmitPredictionRequest {

        @NotBlank(message = "종목 코드는 필수입니다.")
        private String stockCode;

        @NotNull(message = "예측 기간은 필수입니다.")
        private PredictionDuration duration;

        @NotNull(message = "예측 구간은 필수입니다.")
        private PredictionTarget target;
    }
}