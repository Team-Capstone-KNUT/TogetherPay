package com.devcrew.togetherpay.domain.team.controller;

import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.dto.CreateTeamRequest;
import com.devcrew.togetherpay.domain.team.dto.JoinTeamRequest;
import com.devcrew.togetherpay.domain.team.dto.TeamResponse;
import com.devcrew.togetherpay.domain.team.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;
    /**
     * 팀 생성 API
     */
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateTeamRequest request) {
        Team team = teamService.createTeam(userId, request.name(), request.password());
        TeamResponse response = TeamResponse.from(team);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PostMapping("/join")
    public ResponseEntity<Void> joinTeam(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody JoinTeamRequest request) {
        teamService.joinTeam(userId, request.inviteCode(), request.password());

        return ResponseEntity.ok().build();
    }
}
