package com.devcrew.togetherpay.domain.team.dto;

import com.devcrew.togetherpay.domain.team.Team;

import java.util.List;

public record FindTeamsResponse(
        List<TeamItemResponse> teams
) {
    public static FindTeamsResponse of(List<Team> teams, Long myUserId) {
        return new FindTeamsResponse(
                teams.stream()
                        .map(team -> TeamItemResponse.of(team, myUserId))
                        .toList()
        );
    }
}