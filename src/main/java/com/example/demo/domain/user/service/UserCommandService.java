package com.example.demo.domain.user.service;

import com.example.demo.domain.user.entity.User;

public interface UserCommandService {
    User registerUser(User user);
    User updateUser(Long userId, String nickname, String profileImg, String accountNumber);
    Long deleteUser(Long userId);
}
