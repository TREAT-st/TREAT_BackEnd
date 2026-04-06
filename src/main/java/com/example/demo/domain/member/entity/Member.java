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

    private String name;

    private String nickname;


    public static Member of(MemberRequest.MemberRegisterDto memberRequest) {
        return Member.builder()
                .profileImg(memberRequest.getProfileImg())
                .name(memberRequest.getName())
                .nickname(memberRequest.getNickname())
                .kakaoEmail(memberRequest.getKakaoEmail())
                .build();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void modifyInfo(MemberRequest.MemberModifyDto memberRequest) {
        this.name = memberRequest.getName();
        this.nickname = memberRequest.getNickname();
        this.profileImg = memberRequest.getProfileImg();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER")); // TODO: 임의의 ROLE_USER 값 부여하였으므로 ROLE이 필요할 경우 추후 enum 타입으로 수정 예정
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
