package com.example.demo.domain.userDevice;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.user.entity.*;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.domain.userDevice.entity.*;
import com.example.demo.domain.userDevice.repository.UserDeviceRepository;
import com.example.demo.domain.userDevice.service.UserDeviceCommandService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:devices;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({UserDeviceCommandService.class, JpaAuditingConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserDeviceIntegrationTest {
    @Autowired UserDeviceCommandService service;
    @Autowired UserDeviceRepository devices;
    @Autowired UserRepository users;
    User first;
    User second;

    @BeforeEach
    void setUp() {
        devices.deleteAll();
        first = user();
        second = user();
    }

    private User user() {
        return users.save(User.builder().username(UUID.randomUUID().toString())
                .role(Role.values()[0]).status(UserStatus.ACTIVE).build());
    }

    @Test
    void registersMultipleDevicesAndRefreshesTokenWithoutDuplicatingRow() {
        var initial = service.register(first, "install-1", "token-1", DevicePlatform.ANDROID);
        service.register(first, "install-2", "token-2", DevicePlatform.IOS);
        var updated = service.register(first, "install-1", "token-new", DevicePlatform.ANDROID);
        assertThat(devices.count()).isEqualTo(2);
        assertThat(updated.getId()).isEqualTo(initial.getId());
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(initial.getUpdatedAt());
        assertThat(devices.findById(initial.getId()).orElseThrow().getFcmToken()).isEqualTo("token-new");
    }

    @Test
    void accountSwitchTransfersOwnershipAndOldLogoutCannotDeleteIt() {
        var initial = service.register(first, "install-1", "token-1", DevicePlatform.ANDROID);
        service.register(second, "install-1", "token-1", DevicePlatform.ANDROID);
        service.unregister(first.getId(), "install-1");
        var stored = devices.findById(initial.getId()).orElseThrow();
        assertThat(stored.getUser().getId()).isEqualTo(second.getId());
        assertThat(devices.count()).isEqualTo(1);
    }

    @Test
    void unregisterIsIdempotentAndOnlyDeletesRequestedDevice() {
        service.register(first, "install-1", "token-1", DevicePlatform.ANDROID);
        service.register(first, "install-2", "token-2", DevicePlatform.IOS);
        service.unregister(first.getId(), "install-1");
        service.unregister(first.getId(), "install-1");
        assertThat(devices.findAll()).singleElement()
                .satisfies(d -> assertThat(d.getInstallationId()).isEqualTo("install-2"));
    }

    @Test
    void sameTokenOnDifferentInstallationIsRejectedAndOriginalIsPreserved() {
        var original = service.register(first, "install-1", "token-1", DevicePlatform.ANDROID);
        assertThatThrownBy(() -> service.register(second, "install-2", "token-1", DevicePlatform.IOS))
                .isInstanceOf(GeneralException.class);
        assertThat(devices.count()).isEqualTo(1);
        assertThat(devices.findById(original.getId()).orElseThrow().getUser().getId()).isEqualTo(first.getId());
    }
}
