package com.example.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    USER,   // 일반 사용자
    ADMIN;   // 관리자
}
