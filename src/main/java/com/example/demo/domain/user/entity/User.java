package com.example.demo.domain.user.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
public class User extends BaseTimeEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    // ========== Member에서 온 것 (JWT/Security) ==========

    @Column(unique = true, nullable = false)
    private String username;        // JWT subject 식별자

    @Column(unique = true)
    private String kakaoEmail;      // 카카오 이메일 (nullable — 추후 다른 provider 확장 대비)

    private String profileImg;

    // ========== User 도메인 필드 ==========

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "nickname", unique = true, nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "account_number", unique = true, length = 50)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private SocialProvider provider;

    @Column(name = "provider_user_id", length = 100)
    private String providerUserId;

    // ========== 도메인 메서드 ==========

    public void updateUserInfo(String name, String nickname, String profileImg,
                               LocalDate birthDate, Gender gender, String accountNumber) {
        this.name = name;
        this.nickname = nickname;
        this.profileImg = profileImg;
        this.birthDate = birthDate;
        this.gender = gender;
        this.accountNumber = accountNumber;
    }

    // ========== UserDetails 구현 ==========

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Member의 하드코딩 "ROLE_USER" → User의 Role enum 활용
        return List.of(new SimpleGrantedAuthority(this.role.name()));
    }

    @Override
    public String getPassword() {
        return null; // OAuth2 기반, 비밀번호 없음
    }

    @Override
    public String getUsername() {
        return this.username; // JWT subject
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        // Member의 하드코딩 true → User의 UserStatus 활용
        return this.status == UserStatus.ACTIVE;
    }
}