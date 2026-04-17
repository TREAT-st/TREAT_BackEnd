package com.example.demo.domain.userPortfolio.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "userPortfolio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPortfolio extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_point", nullable = false)
    private Long totalPoint = 0L;

    @Column(name = "total_prediction", nullable = false)
    private Long totalPrediction = 0L;

    @Column(name = "success_count", nullable = false)
    private Long successCount = 0L;

    @Column(name = "fail_count", nullable = false)
    private Long failCount = 0L;

    @Column(name = "virtual_profit_krw", nullable = false)
    private Double virtualProfitKrw = 0.0;

    @Column(name = "virtual_profit_percent", nullable = false)
    private Double virtualProfitPercent = 0.0;
}