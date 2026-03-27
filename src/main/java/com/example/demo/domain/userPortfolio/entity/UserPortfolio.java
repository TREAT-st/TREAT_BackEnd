package com.example.demo.domain.userPortfolio.entity;

import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Integer portfolioId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_point", nullable = false)
    private Integer totalPoint;

    @Column(name = "total_prediction", nullable = false)
    private Integer totalPrediction;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "fail_count", nullable = false)
    private Integer failCount;

    @Column(name = "virtual_profit", nullable = false)
    private Double virtualProfit;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public UserPortfolio(User user, Integer totalPoint, Integer totalPrediction,
                         Integer successCount, Integer failCount, Double virtualProfit) {
        this.user = user;
        this.totalPoint = totalPoint;
        this.totalPrediction = totalPrediction;
        this.successCount = successCount;
        this.failCount = failCount;
        this.virtualProfit = virtualProfit;
    }
}