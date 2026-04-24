package com.example.demo.domain.favoriteStock.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

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
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FavoriteStock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_stock_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    @Column(name = "is_alert_enabled", nullable = false)
    private Boolean isAlertEnabled = false;

    public void setIsAlertEnabled(boolean alertEnabled) {
        this.isAlertEnabled = alertEnabled;
    }
}