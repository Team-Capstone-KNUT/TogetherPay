package com.devcrew.togetherpay.domain.team;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeamRole {
    LEADER("팀장"),
    MEMBER("팀원");

    private final String description;
}