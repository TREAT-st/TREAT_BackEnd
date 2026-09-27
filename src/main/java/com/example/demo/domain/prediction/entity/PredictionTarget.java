package com.example.demo.domain.prediction.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PredictionTarget {

    UNDER_3(1) {
        @Override
        public boolean isHit(double ratePercent) {
            return Math.abs(ratePercent) < 3.0;
        }
    },
    BETWEEN_3_5(2) {
        @Override
        public boolean isHit(double ratePercent) {
            return Math.abs(ratePercent) >= 3.0 && Math.abs(ratePercent) < 5.0;
        }
    },
    OVER_5(3) {
        @Override
        public boolean isHit(double ratePercent) {
            return Math.abs(ratePercent) >= 5.0;
        }
    };

    private final int pointWeight;

    public abstract boolean isHit(double ratePercent);
}