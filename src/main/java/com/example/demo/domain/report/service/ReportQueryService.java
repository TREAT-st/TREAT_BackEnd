package com.example.demo.domain.report.service;

import com.example.demo.domain.report.entity.Report;

public interface ReportQueryService {
    Report getReportByReportRequestId(Long reportRequestId);
}
