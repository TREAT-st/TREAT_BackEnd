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

    /** 직전 거래일 종가 대비 등락폭(원). 하락이면 음수. */
    @Column(name = "price_change")
    private BigDecimal priceChange;

    /** 직전 거래일 종가 대비 등락률(%). 소수 둘째 자리. 하락이면 음수. */
    @Column(name = "change_rate", precision = 9, scale = 2)
    private BigDecimal changeRate;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Builder.Default
    @Column(name = "like_count", nullable = false, updatable = false)
    private Long likeCount = 0L;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 한 거래일의 시세를 통째로 반영한다. 시가총액·등락은 모두 그 거래일 기준이라
     * 따로 갱신하면 서로 다른 날짜의 값이 섞인다.
     */
    public void updatePrice(StockPriceSnapshot snapshot, LocalDate tradeDate) {
        this.openPrice = snapshot.openPrice();
        this.closePrice = snapshot.closePrice();
        this.marketCapitalization = snapshot.marketCapitalization();
        this.priceChange = snapshot.priceChange();
        this.changeRate = snapshot.changeRate();
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
