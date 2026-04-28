package com.example.demo.domain.user.service;

import com.example.demo.domain.user.entity.Gender;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.exception.UserHandler;
import com.example.demo.domain.user.repository.UserRepository;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;

    // TODO: builder로 처리하기
    @Override
    public User registerUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long userId, String name, String nickname, String profileImg,
                           LocalDate birthDate, Gender gender, String accountNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserHandler.NOT_FOUND);
        user.updateUserInfo(name, nickname, profileImg, birthDate, gender, accountNumber);
        return user;
    }

    /*
        TODO: 1. hard delete, soft delete 따로 구현하기.
              2. User 삭제 시, 연결된 데이터 연쇄 삭제 생각해야함.
    */
    @Override
    public Long deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserHandler.NOT_FOUND);
        userRepository.delete(user);
        return user.getId();
    }
}
