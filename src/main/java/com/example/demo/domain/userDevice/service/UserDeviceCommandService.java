package com.example.demo.domain.userDevice.service;

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.userDevice.entity.*;
import com.example.demo.domain.userDevice.repository.UserDeviceRepository;
import com.example.demo.domain.userDevice.exception.UserDeviceHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserDeviceCommandService {
    private final UserDeviceRepository repository;

    public UserDevice register(User user, String installationId, String token, DevicePlatform platform) {
        UserDevice device = repository.findByInstallationIdForUpdate(installationId)
                .orElseGet(() -> UserDevice.builder().installationId(installationId).build());
        device.register(user, token, platform);
        try {
            return repository.saveAndFlush(device);
        } catch (DataIntegrityViolationException e) {
            // Concurrent first registration or a token already bound to another installation.
            throw UserDeviceHandler.CONFLICT;
        }
    }

    public void unregister(Long userId, String installationId) {
        // A delayed logout from the previous account must not delete the new owner's device.
        repository.deleteOwnedDevice(installationId, userId);
    }
}
