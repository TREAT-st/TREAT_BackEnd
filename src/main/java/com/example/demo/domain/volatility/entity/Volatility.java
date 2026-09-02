package com.example.demo.domain.volatility.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(
        name = "volatility",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_volatility_stock_code_trade_date",
                columnNames = {"stock_code", "trade_date"}),
        indexes = @Index(name = "idx_volatility_trade_date", columnList = "trade_date")
)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Volatility extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "volatility_id")
    private Long id;

    @Column(name = "stock_name", nullable = false, length = 50)
    private String stockName;

    @Pattern(regexp = "\\d{6}")
    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    /**
     * KRX 응답의 실제 거래일. createdDate(서버 시각)를 날짜 키로 쓰면 타임존·자정 경계에서
     * 저장 날짜와 실제 거래일이 어긋나므로, 날짜 조회는 모두 이 컬럼을 기준으로 한다.
     */
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "report_url")
    private String reportUrl;

    public void updateReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public void updateStockName(String stockName) {
        this.stockName = stockName;
    }
}
