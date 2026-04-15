package com.example.demo.domain.user.repository;

import com.example.demo.domain.user.entity.SocialProvider;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
    boolean existsUserByNickname(String nickname);
}
