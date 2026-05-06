package com.example.demo.api.user.service;

import com.example.demo.api.user.dto.UserRequestDto.UpdateUserRequest;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.service.UserCommandService;
import com.example.demo.domain.userPortfolio.service.UserPortfolioCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class UserUseCase {
    private final UserCommandService userCommandService;
    private final UserPortfolioCommandService userPortfolioCommandService;

    public User signUpWithUserPortfolio(User user) {
        User registeredUser = userCommandService.registerUser(user);
        userPortfolioCommandService.createPortfolio(registeredUser);

        return registeredUser;
    }

    public User editUserAccount(Long userId, UpdateUserRequest request) {
        return userCommandService.updateUser(
                userId,
                request.getName(),
                request.getNickname(),
                request.getProfileImg(),
                request.getBirthDate(),
                request.getGender(),
                request.getAccountNumber()
        );
    }
}
