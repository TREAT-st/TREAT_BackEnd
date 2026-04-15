package com.example.demo.domain.reportRequest.service;

import com.example.demo.domain.reportRequest.entity.ReportRequest;

public interface ReportRequestCommandService {
    ReportRequest createReportRequest(ReportRequest reportRequest);
    Long deleteReportRequest(Long requestId);
}
