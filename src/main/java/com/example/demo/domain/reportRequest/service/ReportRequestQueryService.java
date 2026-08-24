package com.example.demo.domain.reportRequest.service;

import com.example.demo.domain.reportRequest.entity.ReportRequest;
import com.example.demo.domain.reportRequest.entity.RequestStatus;

import java.util.List;

public interface ReportRequestQueryService {
    ReportRequest findById(Long requestId);
    List<ReportRequest> findAllByFavoriteStockId(Long favoriteStockId);
    List<ReportRequest> findAllByUserId(Long userId);
    List<ReportRequest> findAllByRequestStatus(RequestStatus requestStatus);
}
