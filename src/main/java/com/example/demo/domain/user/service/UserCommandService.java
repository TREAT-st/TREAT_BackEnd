package com.example.demo.domain.user.service;

import com.example.demo.domain.user.entity.Gender;
import com.example.demo.domain.user.entity.User;

import java.time.LocalDate;

public interface UserCommandService {
    User registerUser(User user);
    User updateUser(Long userId, String nickname, String profileImg, String accountNumber);
    Long deleteUser(Long userId);
}
