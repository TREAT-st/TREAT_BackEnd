package com.example.demo.domain.prediction.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Prediction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prediction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration", nullable = false)
    private PredictionDuration duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "target", nullable = false)
    private PredictionTarget target;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PredictionStatus status = PredictionStatus.PENDING;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "maturity_at", nullable = false)
    private LocalDateTime maturityAt;

    @Column(name = "earnable_points", nullable = false)
    private Long earnablePoints;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    public void markGradingFailed() {
        this.status = PredictionStatus.FAILED;
    }

    public void grade(PredictionStatus result) {
        this.status = result;
        this.gradedAt = LocalDateTime.now();
    }
}