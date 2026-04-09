package com.example.demo.domain.member.dto;

import com.example.demo.domain.member.entity.Member;
import com.example.demo.domain.member.entity.Role;
import lombok.Builder;
import lombok.Getter;

public class MemberResponse {

    @Getter
    @Builder
    public static class MemberInfoDto {
        private Long id;
        private String username;
        private String kakaoEmail;
        private String profileImg;
        private Role role;

        public static MemberInfoDto from(Member member) {
            return MemberInfoDto.builder()
                    .id(member.getId())
                    .username(member.getUsername())
                    .kakaoEmail(member.getKakaoEmail())
                    .profileImg(member.getProfileImg())
                    .role(member.getRole())
                    .build();
        }
    }
}