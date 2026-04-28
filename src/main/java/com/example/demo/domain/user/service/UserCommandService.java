package com.example.demo.domain.user.service;

import com.example.demo.domain.user.entity.Gender;
import com.example.demo.domain.user.entity.User;

import java.time.LocalDate;

public interface UserCommandService {
    User registerUser(User user);
    User updateUser(Long userId, String name, String nickname, String profileImg,
                    LocalDate birthDate, Gender gender, String accountNumber);
    Long deleteUser(Long userId);
}
