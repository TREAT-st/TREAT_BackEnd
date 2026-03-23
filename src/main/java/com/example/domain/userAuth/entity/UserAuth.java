package com.example.domain.userAuth.entity;

import com.example.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_auth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_auth_id")
    private Integer userAuthId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false)
    private LoginType loginType;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "provider_login_id", length = 100)
    private String providerLoginId;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified;

    @Column(name = "created_at", nullable = false, updatable = false,
            insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserAuth(User user, LoginType loginType, String email, String password,
                    String providerLoginId, Boolean isEmailVerified) {
        this.user = user;
        this.loginType = loginType;
        this.email = email;
        this.password = password;
        this.providerLoginId = providerLoginId;
        this.isEmailVerified = isEmailVerified;
    }
}
