package com.example.demo.domain.userDevice.repository;

import com.example.demo.domain.userDevice.entity.UserDevice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    List<UserDevice> findAllByUserId(Long userId);

    // 발송 이후 토큰 갱신 또는 계정 변경이 있었다면 현재 연결을 보존합니다.
    @Modifying
    @Query("delete from UserDevice d where d.id = :deviceId and d.user.id = :userId "
            + "and d.fcmToken = :token and d.updatedAt = :updatedAt")
    int deleteExpiredToken(@Param("deviceId") Long deviceId, @Param("userId") Long userId,
                           @Param("token") String token, @Param("updatedAt") LocalDateTime updatedAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from UserDevice d where d.installationId = :installationId")
    Optional<UserDevice> findByInstallationIdForUpdate(@Param("installationId") String installationId);

    @Modifying
    @Query("delete from UserDevice d where d.installationId = :installationId and d.user.id = :userId")
    int deleteOwnedDevice(@Param("installationId") String installationId, @Param("userId") Long userId);
}
