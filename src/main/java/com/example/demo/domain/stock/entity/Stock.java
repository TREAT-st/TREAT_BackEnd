package com.example.demo.domain.stock.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Stock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long id;

    @Column(name = "stock_code", nullable = false, unique = true, length = 20)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "market_capitalization")
    private Long marketCapitalization;

    @Column(name = "open_price")
    private BigDecimal openPrice;

    @Column(name = "close_price")
    private BigDecimal closePrice;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Builder.Default
    @Column(name = "like_count")
    private Long likeCount = 0L;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 시가총액은 종가 × 상장주식수라 시세와 같은 거래일에 묶여야 의미가 있다.
     * 따로 갱신하면 어느 날짜 기준인지 알 수 없어진다.
     */
    public void updatePrice(BigDecimal openPrice, BigDecimal closePrice,
                            Long marketCapitalization, LocalDate tradeDate) {
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.marketCapitalization = marketCapitalization;
        this.tradeDate = tradeDate;
    }

    public void updateName(String stockName) {
        this.stockName = stockName;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
