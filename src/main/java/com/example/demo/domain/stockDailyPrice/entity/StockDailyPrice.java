package com.example.demo.domain.stockDailyPrice.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 종목의 일별 시세(일봉). 차트 데이터의 원천이다.
 *
 * Stock과 분리한 이유는 행의 단위가 달라서다. Stock은 종목당 1행을 매일 덮어쓰고(목록·상세용 최신 값),
 * 이 테이블은 종목 × 거래일마다 1행씩 쌓는다(기간 조회용). 차트는 모든 기간을 이 일봉으로 그린다.
 *
 * stock_id FK 대신 종목코드로 가리킨다. 조회가 항상 종목코드로 들어오고(/stocks/{stockCode}/chart),
 * 백필로 3년치(약 15만 행)를 넣을 때 Stock 엔티티를 하나하나 불러올 필요가 없다.
 * stock 행은 지우지 않고 비활성 처리만 하므로 고아 행이 생기지 않는다.
 *
 * (stock_code, trade_date) 유니크 제약이 같은 날 중복 적재를 막고, 기간 조회 인덱스로도 쓰인다.
 * 수정주가 보정은 하지 않는다. Stock의 시세가 보정 전 값이라, 목록의 종가와 차트의 마지막 봉이 같은 값을 보이도록 맞춘다.
 */
@Entity
@Table(name = "stock_daily_price",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_daily_price_code_date",
                columnNames = {"stock_code", "trade_date"}))
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StockDailyPrice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_daily_price_id")
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "open_price", nullable = false)
    private BigDecimal openPrice;

    /** 고가·저가는 KRX에서 못 받을 수 있다(시장 전체 고저가 조회 실패 등). 시가·종가만으로 선 차트는 그릴 수 있다. */
    @Column(name = "high_price")
    private BigDecimal highPrice;

    @Column(name = "low_price")
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false)
    private BigDecimal closePrice;

    /** 같은 거래일을 다시 받았을 때(재백필, 일일 적재 재시도) 값을 갱신한다. */
    public void update(BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice) {
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
    }
}
