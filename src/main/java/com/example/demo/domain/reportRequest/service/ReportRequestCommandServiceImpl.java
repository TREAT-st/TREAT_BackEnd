package com.example.demo.domain.reportRequest.service;

import com.example.demo.domain.reportRequest.entity.ReportRequest;
import com.example.demo.domain.reportRequest.repository.ReportRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportRequestCommandServiceImpl implements ReportRequestCommandService {

    private final ReportRequestRepository reportRequestRepository;

    //  TODO: builder로 처리하기
    @Override
    public ReportRequest createReportRequest(ReportRequest reportRequest) {
        return reportRequestRepository.save(reportRequest);
    }

    @Override
    public Long deleteReportRequest(Long requestId) {
        ReportRequest request = reportRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("ReportRequest not found: " + requestId));
        reportRequestRepository.delete(request);
        return request.getId();
    }
}
