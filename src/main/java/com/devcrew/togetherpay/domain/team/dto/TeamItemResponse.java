package com.devcrew.togetherpay.domain.team.dto;

import com.devcrew.togetherpay.domain.team.Team;

import java.util.List;

public record TeamItemResponse(
        Long teamId,
        String name,
        String inviteCode,
        String myRole, // ⭐️ 기존에 있던 내 역할 필드 유지!
        List<TeamUserResponse> users
) {
    // ⭐️ 내 역할을 찾기 위해 myUserId도 같이 받습니다.
    public static TeamItemResponse of(Team team, Long myUserId) {

        // 팀 유저 목록을 뒤져서 내 역할을 찾아냅니다. (보통 방장/팀원 구분에 쓰임)
        String myRole = team.getTeamUsers().stream()
                .filter(tu -> tu.getUser().getId().equals(myUserId))
                .findFirst()
                .map(tu -> tu.getRole().name()) // Enum 타입이라면 .name() 사용
                .orElse("MEMBER");

        return new TeamItemResponse(
                team.getId(),
                team.getName(),
                team.getInviteCode(),
                myRole, // 찾은 내 역할 삽입
                team.getTeamUsers().stream()
                        .map(tu -> TeamUserResponse.from(tu.getUser()))
                        .toList()
        );
    }
}