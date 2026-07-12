package com.example.demo.api.prediction.service;

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * User 포인트 지급 어댑터 (3-A).
 * ★ 전제: User 엔티티에 아래가 추가되어 있어야 함
 *     private Integer point;
 *     public void addPoint(int amount) { this.point = (point == null ? 0 : point) + amount; }
 */
@Component
@RequiredArgsConstructor
public class UserPointPort {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user를 찾지 못 했습니다: " + userId));
    }

    @Transactional
    public void addPoint(User user, int amount) {
        user.addPoint(amount);   // dirty checking 으로 반영
    }
}
