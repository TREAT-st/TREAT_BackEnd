package com.example.demo.security.jwt.service;

import com.example.demo.domain.member.entity.Member;
import com.example.demo.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
@RequiredArgsConstructor
@Service("customUserDetailsService")
//TODO : 자체 로그인 구현 안 할 경우 제외 예정
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberQueryService memberQueryService;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member memberByUsername = memberQueryService.getMemberByUsername(username);
        return memberByUsername;
    }
}