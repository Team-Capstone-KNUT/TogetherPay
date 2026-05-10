package com.devcrew.togetherpay.domain.team.dto;

import com.devcrew.togetherpay.domain.team.Team;
import java.util.List;

public record TeamResponse(
        Long teamId,
        String name,
        String inviteCode,
        List<MemberResponse> members
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getInviteCode(),
                // Team 엔티티 내부의 teamUsers를 MemberResponse 리스트로 변환
                team.getTeamUsers().stream()
                        .map(tu -> new MemberResponse(
                                tu.getUser().getId(),
                                tu.getUser().getNickname(),
                                tu.getRole()
                        ))
                        .toList()
        );
    }
}