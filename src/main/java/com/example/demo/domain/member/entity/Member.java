package com.example.demo.domain.member.entity;

import com.example.demo.domain.auditing.entity.BaseTimeEntity;
import com.example.demo.domain.member.dto.MemberRequest;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(name = "member")
public class Member extends BaseTimeEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", updatable = false, unique = true, nullable = false)
    private Long id;

    @Column(unique = true, nullable = false)
    private String kakaoEmail;

    @Column(unique = true, nullable = false)
    private String username;

    private String profileImg;
    //TODO: 카카오에서 닉네임 받아서 그대로 사용하는건지 / 닉네임 입력칸을 따로 만드는 건지 확인
    //private String name;

    //private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;


    public static Member of(MemberRequest.MemberRegisterDto memberRequest) {
        return Member.builder()
                .profileImg(memberRequest.getProfileImg())
                .username(memberRequest.getName())
                .kakaoEmail(memberRequest.getKakaoEmail())
                .build();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void modifyInfo(MemberRequest.MemberModifyDto memberRequest) {
        this.username = memberRequest.getName();
        this.profileImg = memberRequest.getProfileImg();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(this.role.getKey()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
