package com.example.demo.security.oauth.factory;

import com.example.demo.security.oauth.dto.KakaoOAuth2User;

import java.util.Map;
//TODO: 구글, 네이버 같은 다른 소셜 로그인을 추가할 때 분기 처리하기 위해서 만든 구조
public class OAuth2UserInfoFactory {
    public static KakaoOAuth2User getOAuth2UserInfo(Map<String, Object> attributes) {
        return new KakaoOAuth2User(attributes);
    }
}
