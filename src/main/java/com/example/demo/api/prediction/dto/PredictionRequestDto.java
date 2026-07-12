package com.example.demo.api.prediction.dto;

import com.example.demo.domain.prediction.entity.PredictionDuration;
import com.example.demo.domain.prediction.entity.PredictionTarget;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class PredictionRequestDto {

    @Getter
    @NoArgsConstructor
    public static class CreatePrediction {

        @NotNull(message = "종목코드는 필수입니다.")
        private String stockCode;

        @NotNull(message = "예측 기간은 필수입니다.")
        private PredictionDuration duration;

        @NotNull(message = "예측 상승폭은 필수입니다.")
        private PredictionTarget target;
    }

    /**
     * 데모/테스트용 수동 채점 요청. 실서비스는 스케줄러가 처리하므로 이 값 불필요.
     */
    @Getter
    @NoArgsConstructor
    public static class GradePrediction {

        @NotNull(message = "만기 종가는 필수입니다.")
        private BigDecimal maturityPrice;
    }
}