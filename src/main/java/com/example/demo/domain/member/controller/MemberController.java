package com.example.demo.domain.member.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.domain.member.dto.MemberResponse;
import com.example.demo.domain.member.entity.Member;
import com.example.demo.domain.member.service.MemberQueryService;
import com.example.demo.security.oauth.dto.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member API", description = "멤버 조회 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberQueryService memberQueryService;

    @Operation(summary = "내 정보 조회", description = "로그인한 멤버의 정보를 조회합니다. (JWT 필요)")
    @GetMapping("/info")
    public ApiResponseDto<MemberResponse.MemberInfoDto> getMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = memberQueryService.getMemberById(userDetails.getId());
        return ApiResponseDto.onSuccess(MemberResponse.MemberInfoDto.from(member));
    }

}