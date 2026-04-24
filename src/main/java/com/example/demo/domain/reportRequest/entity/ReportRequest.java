package com.example.demo.domain.reportRequest.entity;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_request")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReportRequest extends BaseTimeEntity {

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
    private RequestStatus requestStatus = RequestStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    public void startProcessing() {
        this.requestStatus = RequestStatus.IN_PROGRESS;
    }

    public void complete() {
        this.requestStatus = RequestStatus.DONE;
        this.completedDate = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.requestStatus = RequestStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }
}
