package com.example.demo.domain.report.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import com.example.demo.domain.reportRequest.entity.ReportRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_request_id")
    private ReportRequest reportRequest;

    @Column(name = "report_html_url", nullable = false)
    private String reportHtmlUrl;

    @Column(name = "report_pdf_url", nullable = false)
    private String reportPdfUrl;
}
