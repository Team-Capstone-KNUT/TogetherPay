package com.devcrew.togetherpay.domain.user.dto;

import com.devcrew.togetherpay.domain.user.User;

public record UserProfileResponse(
        Long id,
        String email,
        String nickname,
        String provider
) {
    // 엔티티 DTO 변환 메서드 정의
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider().name()
        );
    }
}
