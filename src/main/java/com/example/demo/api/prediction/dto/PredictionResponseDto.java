package com.example.demo.api.prediction.dto;

import com.example.demo.domain.prediction.entity.PredictionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public class PredictionResponseDto {

    /** 예측 화면 진입 - 종목 정보 (Image 1 상단 카드) */
    @Getter
    @Builder
    public static class PredictionInfo {
        private String stockCode;
        private String stockName;
        private BigDecimal currentPrice;   // 현재가
        private BigDecimal changeAmount;   // 전일 대비 상승분
        private BigDecimal changeRate;     // 상승률(%)
    }

    /** 예측 제출 완료 응답 (아직 결과 아님) */
    @Getter
    @Builder
    public static class CreatePredictionResult {
        private Long predictionId;
        private Integer possiblePoint;     // 획득 가능 포인트
        private PredictionStatus status;   // PENDING
    }

    /** 예측 결과 조회 (Image 2 / Image 3) */
    @Getter
    @Builder
    public static class PredictionResult {
        private Long predictionId;
        private PredictionStatus status;   // PENDING / CORRECT / WRONG
        private String myPredictionText;   // "3%미만 상승"
        private BigDecimal actualRate;     // 실제 등락률(%). PENDING이면 null
        private Integer earnedPoint;       // 지급 포인트. PENDING이면 null
        private String title;              // "예측 적중!" / "예측이 빗나갔어요..."
        private String subtitle;
        private String reasonHeader;       // "왜 올랐을까?" / "왜 떨어졌을까?"
        private List<Reason> reasons;      // 더미 데이터 (2-C)
    }

    @Getter
    @Builder
    public static class Reason {
        private String title;
        private String content;
    }
}
