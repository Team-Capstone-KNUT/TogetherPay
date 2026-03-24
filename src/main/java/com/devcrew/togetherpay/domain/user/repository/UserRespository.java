package com.devcrew.togetherpay.domain.user.repository;

import com.devcrew.togetherpay.domain.user.Provider;
import com.devcrew.togetherpay.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRespository extends JpaRepository<User, Long> {
    // 소셜 로그인 시, 이메일로 기존 가입자인지 검증용으로 사용
    Optional<User> findByEmail(String email);
    // 소셜 로그인의 경우 이메일이 없는 경우가 있어, 소셜 제공자 타입 AND 식별자로 조회
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
