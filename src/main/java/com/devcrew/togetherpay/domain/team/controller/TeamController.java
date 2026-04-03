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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;

    /**
     * 팀 생성 API
     * @param userId
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateTeamRequest request) {
        // 팀 비즈니스 로직에서 생성 메서드 호출하고 파라미터로 userId, 받은 request 안에 들어있는 name, password를 넘겨준다.
        Team team = teamService.createTeam(userId, request.name(), request.password());
        // 응답값 response from 메서드 호출해서 자동 변환
        TeamResponse response = TeamResponse.from(team);
        // response에 http 상태값도 같이 body에 담아서 전달
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 팀 가입 API
     * @param userId
     * @param request
     * @return
     */
    @PostMapping("/join")
    public ResponseEntity<Void> joinTeam(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody JoinTeamRequest request) {
        // 비즈니스 로직 단 팀 참가 메서드 호출, 파라미터로 userId, request 내의 inviteCode(초대코드), password(패스워드) 반환
        teamService.joinTeam(userId, request.inviteCode(), request.password());

        return ResponseEntity.ok().build();
    }

    /**
     * 팀 삭제 API
     * @param userId
     * @param teamId
     * @return
     */
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId) {
        // 팀 삭제 비즈니스 로직 호출, 파라미터로 userId와 teamId를 넘겨준다.
        teamService.deleteTeam(userId, teamId);
        // 응답 성공 시 204 no content 반환(반환값 없음)
        return ResponseEntity.noContent().build();
    }
}
