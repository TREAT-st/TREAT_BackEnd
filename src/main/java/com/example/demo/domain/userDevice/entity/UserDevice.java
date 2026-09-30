package com.example.demo.domain.userDevice.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_device", uniqueConstraints = {
        @UniqueConstraint(name = "uk_device_installation", columnNames = "installation_id"),
        @UniqueConstraint(name = "uk_device_fcm_token", columnNames = "fcm_token")
}, indexes = @Index(name = "idx_device_user", columnList = "user_id"))
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_device_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "installation_id", nullable = false, length = 128)
    private String installationId;

    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void register(User user, String token, DevicePlatform platform) {
        this.user = user;
        this.fcmToken = token;
        this.platform = platform;
        this.updatedAt = LocalDateTime.now();
    }
}
