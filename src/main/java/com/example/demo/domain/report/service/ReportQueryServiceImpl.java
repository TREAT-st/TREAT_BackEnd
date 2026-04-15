package com.example.demo.domain.report.service;

import com.example.demo.domain.report.entity.Report;
import com.example.demo.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportQueryServiceImpl implements ReportQueryService {
    private final ReportRepository reportRepository;

    @Override
    public Report getReportByReportRequestId(Long reportRequestId) {
        return reportRepository.findByReportRequestId(reportRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found for request: " + reportRequestId));
    }
}
