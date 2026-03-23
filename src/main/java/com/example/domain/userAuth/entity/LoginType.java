package com.example.domain.userAuth.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoginType {
    EMAIL,   // 이메일 로그인
    SOCIAL_LOGIN;   // 소셜 로그인
}
