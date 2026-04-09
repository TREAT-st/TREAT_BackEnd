package com.example.demo.domain.reportRequest.entity;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportRequest")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favorite_stock_id")
    private FavoriteStock favoriteStock;

    @Column(name = "request_stock_code", nullable = false)
    private String requestStockCode;

    @Column(name = "request_ticker", nullable = false)
    private String requestTicker;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false)
    private RequestStatus requestStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;
}
