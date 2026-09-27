package com.example.demo.domain.userDevice.repository;

import com.example.demo.domain.userDevice.entity.UserDevice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from UserDevice d where d.installationId = :installationId")
    Optional<UserDevice> findByInstallationIdForUpdate(@Param("installationId") String installationId);

    @Modifying
    @Query("delete from UserDevice d where d.installationId = :installationId and d.user.id = :userId")
    int deleteOwnedDevice(@Param("installationId") String installationId, @Param("userId") Long userId);
}
