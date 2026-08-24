package com.example.demo.domain.report.repository;

import com.example.demo.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByReportRequestId(Long reportRequestId);
}
