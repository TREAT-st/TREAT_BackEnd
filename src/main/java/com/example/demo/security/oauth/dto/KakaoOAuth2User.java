package com.example.demo.security.oauth.dto;

import java.util.Map;

public class KakaoOAuth2User extends OAuth2UserInfo {
    private Long id;
    private String nickname;
    private String profileImg;

    //카카오 API 응답과 프로젝트 entity 매칭
    public KakaoOAuth2User(Map<String, Object> attributes) {
        super((Map<String, Object>) attributes.get("kakao_account"));
        this.id = (Long) attributes.get("id");
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        this.nickname = (String) profile.get("nickname");
        this.profileImg = (String) profile.get("profile_image_url");

        System.out.println("nickname = " + this.nickname);
    }

    @Override
    public String getOAuth2Id() {
        return this.id.toString();
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() {
        return this.nickname;
    }

    @Override
    public String getProfileImg(){return this.profileImg;}

}