package com.example.demo.domain.reportRequest.service;

import com.example.demo.domain.reportRequest.entity.ReportRequest;

import java.util.List;

public interface ReportRequestQueryService {
    ReportRequest findById(Long requestId);
    List<ReportRequest> findAllByFavoriteStockId(Long favoriteStockId);
}
