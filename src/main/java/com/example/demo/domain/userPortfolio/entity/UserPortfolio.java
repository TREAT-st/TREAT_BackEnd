package com.example.demo.domain.userPortfolio.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "userPortfolio")
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPortfolio extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "total_point", nullable = false)
    private Long totalPoint = 0L;

    @Builder.Default
    @Column(name = "total_prediction", nullable = false)
    private Long totalPrediction = 0L;

    @Builder.Default
    @Column(name = "success_count", nullable = false)
    private Long successCount = 0L;

    @Builder.Default
    @Column(name = "fail_count", nullable = false)
    private Long failCount = 0L;

    @Builder.Default
    @Column(name = "virtual_profit_krw", nullable = false)
    private Double virtualProfitKrw = 0.0;

    @Builder.Default
    @Column(name = "virtual_profit_percent", nullable = false)
    private Double virtualProfitPercent = 0.0;

    public void updateUserPortfolio(Long totalPoint, Long totalPrediction, Long successCount,
                       Long failCount, Double virtualProfitKrw, Double virtualProfitPercent) {
        this.totalPoint = totalPoint;
        this.totalPrediction = totalPrediction;
        this.successCount = successCount;
        this.failCount = failCount;
        this.virtualProfitKrw = virtualProfitKrw;
        this.virtualProfitPercent = virtualProfitPercent;
    }
}