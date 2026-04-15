package com.example.demo.domain.reportRequest.entity;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favorite_stock_id", nullable = false)
    private FavoriteStock favoriteStock;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false)
    private RequestStatus requestStatus;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "requested_date")
    private LocalDateTime requestedDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;
}
