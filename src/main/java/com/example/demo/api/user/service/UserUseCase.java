package com.example.demo.api.user.service;

import com.example.demo.api.user.dto.UserRequestDto.UpdateUserRequest;
import com.example.demo.api.user.dto.UserResponseDto.UserResponse;
import com.example.demo.api.user.mapper.UserConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.service.UserCommandService;
import com.example.demo.domain.userPortfolio.entity.UserPortfolio;
import com.example.demo.domain.userPortfolio.service.UserPortfolioCommandService;
import com.example.demo.domain.userPortfolio.service.UserPortfolioQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class UserUseCase {
    private final UserCommandService userCommandService;
    private final UserPortfolioCommandService userPortfolioCommandService;
    private final UserPortfolioQueryService userPortfolioQueryService;

    public User signUpWithUserPortfolio(User user) {
        User registeredUser = userCommandService.registerUser(user);
        userPortfolioCommandService.createPortfolio(registeredUser);

        return registeredUser;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserInfo(User user) {
        UserPortfolio portfolio = userPortfolioQueryService.getUserPortfolioByUserId(user.getId());
        return UserConverter.toUserResponse(user, portfolio);
    }

    public User editUserAccount(Long userId, UpdateUserRequest request) {
        return userCommandService.updateUser(
                userId,
                request.getNickname(),
                request.getProfileImg(),
                request.getAccountNumber()
        );
    }
}
