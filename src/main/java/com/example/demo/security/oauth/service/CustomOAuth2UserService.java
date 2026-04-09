package com.example.demo.security.oauth.service;

import com.example.demo.common.exception.ErrorStatus;
import com.example.demo.domain.member.entity.Member;
import com.example.demo.domain.member.entity.Role;
import com.example.demo.domain.member.repository.MemberRepository;
import com.example.demo.security.oauth.dto.CustomUserDetails;
import com.example.demo.security.oauth.dto.KakaoOAuth2User;
import com.example.demo.security.oauth.factory.OAuth2UserInfoFactory;
import com.example.demo.common.exception.GeneralException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.Optional;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final MemberRepository memberRepository;
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = oAuth2UserService.loadUser(userRequest);

        return processOAuth2User(oAuth2User, userRequest.getAccessToken());
    }
    protected OAuth2User processOAuth2User(OAuth2User oAuth2User, OAuth2AccessToken oAuth2AccessToken) {
        //OAuth2 로그인 플랫폼 구분 과정 생략, 카카오에서 필요한 정보 가져오기(이메일)
        KakaoOAuth2User oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(oAuth2User.getAttributes());
        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new GeneralException(ErrorStatus.AUTH_OAUTH2_EMAIL_NOT_FOUND_FROM_PROVIDER);
        }
        Optional<Member> byEmail = memberRepository.findByKakaoEmail(oAuth2UserInfo.getEmail());
        Member member = byEmail.orElseGet(() -> registerMember(oAuth2UserInfo));

        return CustomUserDetails.create(member, oAuth2AccessToken);
    }

    private Member registerMember(KakaoOAuth2User oAuth2UserInfo) {
        Member register = Member.builder()
                .kakaoEmail(oAuth2UserInfo.getEmail())
                .username(oAuth2UserInfo.getNickname())
                .profileImg(oAuth2UserInfo.getProfileImg())
                .role(Role.USER)           //회원가입시에만 guest로 두고 이후 사용에는 user로 돌린다
                .build();

        return memberRepository.save(register);
    }


}
