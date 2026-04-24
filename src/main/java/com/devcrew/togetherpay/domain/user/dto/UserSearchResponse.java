package com.devcrew.togetherpay.domain.user.dto;

import com.devcrew.togetherpay.domain.user.User;

public record UserSearchResponse(
        Long userId,
        String nickname,
        String email
) {
    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail()
        );
    }
}
