package com.example.demo.domain.notification.service;

import com.example.demo.domain.favoriteStock.repository.FavoriteStockRepository;
import com.example.demo.domain.notification.entity.NotificationType;
import com.example.demo.domain.notification.repository.NotificationRepository;
import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.repository.VolatilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 리포트가 준비되면 알림을 켠 관심종목 사용자에게 알림 내역과 푸시를 만든다. */
@Service
@RequiredArgsConstructor
public class ReportNotificationService {
    private final VolatilityRepository volatilityRepository;
    private final FavoriteStockRepository favoriteStockRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService notificationDispatchService;

    @Transactional
    public void notifyReportReady(String stockCode, LocalDate tradeDate) {
        Volatility report = volatilityRepository.findByStockCodeAndTradeDate(stockCode, tradeDate)
                .orElseThrow();
        String message = report.getStockName() + " 변동성 리포트가 도착했어요.";

        favoriteStockRepository.findAllByStockCodeAndIsAlertEnabledTrue(stockCode).forEach(favorite -> {
            Long userId = favorite.getUser().getId();
            if (!notificationRepository.existsByUserIdAndSourceIdAndNotificationType(
                    userId, report.getId(), NotificationType.VOLATILITY_REPORT)) {
                notificationDispatchService.create(favorite.getUser(), report.getId(),
                        NotificationType.VOLATILITY_REPORT, "관심 종목 리포트", message);
            }
        });
    }
}
