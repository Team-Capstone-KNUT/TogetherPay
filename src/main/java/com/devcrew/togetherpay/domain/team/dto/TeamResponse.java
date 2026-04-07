package com.devcrew.togetherpay.domain.team.dto;

import com.devcrew.togetherpay.domain.team.Team;

public record TeamResponse(
        Long teamId,
        String name,
        String inviteCode
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getInviteCode()
        );
    }
}
