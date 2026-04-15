package com.example.demo.domain.user.service;

import com.example.demo.domain.user.entity.User;

public interface UserCommandService {
    User registerUser(User user);
    Long deleteUser(Long userId);
}
