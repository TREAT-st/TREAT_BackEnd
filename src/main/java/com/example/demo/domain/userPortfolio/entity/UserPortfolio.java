package com.example.demo.domain.userPortfolio.entity;

import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "userPortfolio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_point", nullable = false)
    private Long totalPoint;

    @Column(name = "total_prediction", nullable = false)
    private Long totalPrediction;

    @Column(name = "success_count", nullable = false)
    private Long successCount;

    @Column(name = "fail_count", nullable = false)
    private Long failCount;

    @Column(name = "virtual_profit_krw", nullable = false)
    private Double virtualProfitKrw;

    @Column(name = "virtual_profit_percent", nullable = false)
    private Double virtualProfitPercent;
}