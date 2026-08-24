package com.example.demo.domain.report.service;

import com.example.demo.domain.report.entity.Report;

public interface ReportCommandService {
    Report createReport(Report report);
    Long deleteReport(Long reportId);
}
