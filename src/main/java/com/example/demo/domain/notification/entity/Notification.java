package com.example.demo.domain.notification.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import com.example.demo.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "message", nullable = false, length = 200)
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    public void markAsRead() {
        this.isRead = true;
    }
}