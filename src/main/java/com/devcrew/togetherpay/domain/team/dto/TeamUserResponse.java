package com.devcrew.togetherpay.domain.team.dto;

import com.devcrew.togetherpay.domain.user.User;

public record TeamUserResponse(
        Long userId,
        String nickname
) {
    public static TeamUserResponse from(User user) {
        return new TeamUserResponse(user.getId(), user.getNickname());
    }
}