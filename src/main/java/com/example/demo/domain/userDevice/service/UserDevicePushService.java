package com.example.demo.domain.userDevice.service;

import com.example.demo.domain.userDevice.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDevicePushService {
    private final UserDeviceRepository repository;

    // 엔티티 대신 발송 시점의 값을 복사해 Firebase 통신 중 DB 트랜잭션을 유지하지 않습니다.
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<PushTarget> getTargets(Long userId) {
        return repository.findAllByUserId(userId).stream()
                .map(d -> new PushTarget(d.getId(), d.getFcmToken(), d.getUpdatedAt()))
                .toList();
    }

    // 정리 실패가 다른 기기의 발송이나 호출자의 DB 작업을 롤백시키지 않도록 분리합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeExpiredToken(Long userId, PushTarget target) {
        repository.deleteExpiredToken(target.deviceId(), userId, target.token(), target.updatedAt());
    }

    public record PushTarget(Long deviceId, String token, LocalDateTime updatedAt) {}
}
