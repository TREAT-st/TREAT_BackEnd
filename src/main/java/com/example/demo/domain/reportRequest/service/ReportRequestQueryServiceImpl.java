package com.example.demo.domain.reportRequest.service;

import com.example.demo.domain.reportRequest.entity.ReportRequest;
import com.example.demo.domain.reportRequest.entity.RequestStatus;
import com.example.demo.domain.reportRequest.exception.ReportRequestHandler;
import com.example.demo.domain.reportRequest.repository.ReportRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportRequestQueryServiceImpl implements ReportRequestQueryService {

    private final ReportRequestRepository reportRequestRepository;

    @Override
    public ReportRequest findById(Long requestId) {
        return reportRequestRepository.findById(requestId)
                .orElseThrow(() -> ReportRequestHandler.NOT_FOUND);
    }

    @Override
    public List<ReportRequest> findAllByFavoriteStockId(Long favoriteStockId) {
        return reportRequestRepository.findAllByFavoriteStockId(favoriteStockId);
    }

    @Override
    public List<ReportRequest> findAllByUserId(Long userId) {
        return reportRequestRepository.findAllByUserId(userId);
    }

    @Override
    public List<ReportRequest> findAllByRequestStatus(RequestStatus requestStatus) {
        return reportRequestRepository.findAllByRequestStatus(requestStatus);
    }
}
