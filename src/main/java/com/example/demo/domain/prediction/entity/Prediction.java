package com.example.demo.domain.prediction.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "prediction")
public class Prediction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PredictionDuration duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PredictionTarget target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PredictionStatus status = PredictionStatus.PENDING;

    @Column(nullable = false)
    private Integer possiblePoint;   // 제출 시점에 계산해 저장 (적중 시 지급액)

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;    // 예측 제출 시점의 기준가

    @Column(nullable = false)
    private LocalDateTime maturityAt; // 채점 만기 시각

    // ---- 채점 후 채워지는 값 ----
    @Column(precision = 15, scale = 2)
    private BigDecimal maturityPrice; // 만기 시점 종가

    @Column(precision = 6, scale = 2)
    private BigDecimal actualRate;    // 실제 등락률 (%)

    private Integer earnedPoint;      // 실제 지급된 포인트

    private LocalDateTime gradedAt;   // 채점 완료 시각

    /**
     * 만기 종가로 채점한다. basePrice 대비 등락률을 구해 target 구간과 비교.
     * @return 지급할 포인트 (적중 시 possiblePoint, 실패 시 0)
     */
    public int grade(BigDecimal maturityPrice) {
        this.maturityPrice = maturityPrice;

        double rate = maturityPrice.subtract(basePrice)
                .divide(basePrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        this.actualRate = BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP);
        this.gradedAt = LocalDateTime.now();

        if (target.matches(rate)) {
            this.status = PredictionStatus.CORRECT;
            this.earnedPoint = this.possiblePoint;
        } else {
            this.status = PredictionStatus.WRONG;
            this.earnedPoint = 0;
        }
        return this.earnedPoint;
    }

    public boolean isPending() {
        return this.status == PredictionStatus.PENDING;
    }
}
