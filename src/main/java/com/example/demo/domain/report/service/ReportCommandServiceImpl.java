package com.example.demo.domain.report.service;

import com.example.demo.domain.report.entity.Report;
import com.example.demo.domain.report.exception.ReportErrorStatus;
import com.example.demo.domain.report.exception.ReportHandler;
import com.example.demo.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportCommandServiceImpl implements ReportCommandService {
    private final ReportRepository reportRepository;

    //  TODO: builder로 처리하기
    @Override
    public Report createReport(Report report) {
        if (reportRepository.findByReportRequestId(report.getReportRequest().getId()).isPresent()) {
            throw new ReportHandler(ReportErrorStatus.REPORT_ALREADY_EXISTS);
        }

        return reportRepository.save(report);
    }

    @Override
    public Long deleteReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> ReportHandler.NOT_FOUND);
        reportRepository.delete(report);
        return report.getId();
    }
}
