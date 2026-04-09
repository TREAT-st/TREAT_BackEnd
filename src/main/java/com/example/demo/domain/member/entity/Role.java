package com.example.demo.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    USER("ROLE_USER"), DORMANT("ROLE_DORMANT"), ADMIN("ROLE_ADMIN");

    private final String key;
}
