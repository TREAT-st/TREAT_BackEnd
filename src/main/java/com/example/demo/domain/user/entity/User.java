package com.example.demo.domain.user.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private SocialProvider provider;

    @Column(name = "provider_user_id", length = 100)
    private String providerUserId;
}