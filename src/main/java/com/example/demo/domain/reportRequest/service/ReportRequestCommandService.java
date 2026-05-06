package com.example.demo.domain.reportRequest.service;

import com.example.demo.domain.reportRequest.entity.ReportRequest;

public interface ReportRequestCommandService {
    ReportRequest createReportRequest(ReportRequest reportRequest);
    void startProcessing(Long requestId);
    void completeRequest(Long requestId);
    void failRequest(Long requestId, String errorMessage);
    Long deleteReportRequest(Long requestId);
}
