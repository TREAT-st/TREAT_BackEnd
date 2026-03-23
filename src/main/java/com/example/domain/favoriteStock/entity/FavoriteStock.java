package com.example.domain.favoriteStock.entity;

import com.example.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorite_stock",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_favorite_stock_user_stock",
                        columnNames = {"user_id", "stock_code"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_stock_id")
    private Integer favoriteStockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "is_alert_enabled", nullable = false)
    private Boolean isAlertEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_period", nullable = false)
    private AlertPeriod alertPeriod;

    @Column(name = "created_at", nullable = false, updatable = false,
            insertable = false)
    private LocalDateTime createdAt;

    @Builder
    public FavoriteStock(User user, String stockCode, Boolean isAlertEnabled,
                         AlertPeriod alertPeriod) {
        this.user = user;
        this.stockCode = stockCode;
        this.isAlertEnabled = isAlertEnabled;
        this.alertPeriod = alertPeriod;
    }
}