package com.example.demo.domain.reportRequest.service;

import com.example.demo.domain.reportRequest.entity.ReportRequest;
import com.example.demo.domain.reportRequest.entity.RequestStatus;
import com.example.demo.domain.reportRequest.exception.ReportRequestErrorStatus;
import com.example.demo.domain.reportRequest.exception.ReportRequestHandler;
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
        if (reportRequest.getRequestStatus() != RequestStatus.PENDING) {
            throw new ReportRequestHandler(ReportRequestErrorStatus.INVALID_INITIAL_STATUS);
        }
        return reportRequestRepository.save(reportRequest);
    }

    @Override
    public void startProcessing(Long requestId) {
        ReportRequest request = reportRequestRepository.findById(requestId)
                .orElseThrow(() -> ReportRequestHandler.NOT_FOUND);
        request.startProcessing();
    }

    @Override
    public void completeRequest(Long requestId) {
        ReportRequest request = reportRequestRepository.findById(requestId)
                .orElseThrow(() -> ReportRequestHandler.NOT_FOUND);
        request.complete();
    }

    @Override
    public void failRequest(Long requestId, String errorMessage) {
        ReportRequest request = reportRequestRepository.findById(requestId)
                .orElseThrow(() -> ReportRequestHandler.NOT_FOUND);
        request.fail(errorMessage);
    }

    @Override
    public Long deleteReportRequest(Long requestId) {
        ReportRequest request = reportRequestRepository.findById(requestId)
                .orElseThrow(() -> ReportRequestHandler.NOT_FOUND);
        reportRequestRepository.delete(request);
        return request.getId();
    }
}
