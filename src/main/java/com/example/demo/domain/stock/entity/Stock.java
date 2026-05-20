package com.example.demo.domain.stock.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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

    @Column(name = "stock_code", nullable = false, unique = true)
    private String stockCode;

    @Column(name = "stock_name",  nullable = false)
    private String stockName;

    @Column(name = "open_price")
    private BigDecimal openPrice;

    @Column(name = "close_price")
    private BigDecimal closePrice;

    @Column(name = "inquiry_date")
    private LocalDate inquiryDate;

    public void updatePrice(BigDecimal openPrice, BigDecimal closePrice, LocalDate inquiryDate) {
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.inquiryDate = inquiryDate;
    }
}
