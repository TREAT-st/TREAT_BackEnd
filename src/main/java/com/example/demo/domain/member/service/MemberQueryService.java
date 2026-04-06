package com.example.demo.domain.member.service;

import com.example.demo.domain.member.entity.Member;

public interface MemberQueryService {
    Member getByKakaoEmail(String kakaoEmail);

    Member getMemberByUsername(String username);

    Member getMemberById(Long memberId);
}
