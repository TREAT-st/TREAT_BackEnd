package com.example.demo.domain.volatility.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "volatility")
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

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "report_url")
    private String reportUrl;
}
