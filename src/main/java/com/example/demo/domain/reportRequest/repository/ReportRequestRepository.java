package com.example.demo.domain.reportRequest.repository;

import com.example.demo.domain.reportRequest.entity.ReportRequest;
import com.example.demo.domain.reportRequest.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRequestRepository extends JpaRepository<ReportRequest, Long> {
    List<ReportRequest> findAllByFavoriteStockId(Long favoriteStockId);
    List<ReportRequest> findAllByUserId(Long userId);
    List<ReportRequest> findAllByRequestStatus(RequestStatus requestStatus);
}
