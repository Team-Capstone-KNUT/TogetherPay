package com.devcrew.togetherpay.domain.team.controller;

import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.dto.*;
import com.devcrew.togetherpay.domain.team.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        Team team = teamService.createTeam(userId, request.name(), request.password(), request.memberIds());
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

    /**
     * 팀 이름 수정 API
     * @param userId
     * @param teamId
     * @param request
     * @return
     */
    @PatchMapping("/{teamId}")
    public ResponseEntity<Void> updateTeamName(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request) {
        // 팀 이름 변경 비즈니스 로직 호출, 파라미터로 userId, teamId, name 넘겨준다.
        teamService.updateTeamName(userId, teamId, request.name());
        // 200 Ok 응답 반환
        return ResponseEntity.ok().build();
    }

    /**
     * 팀 단순 조회
     * @param userId
     * @return
     */
    @GetMapping
    public ResponseEntity<List<TeamSimpleResponse>> getMyTeams(
            @AuthenticationPrincipal Long userId) {
        List<TeamSimpleResponse> response = teamService.getMyTeams(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 팀 조회
     * @param userId
     * @param teamId
     * @return
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> getTeamDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId) {

        TeamDetailResponse response = teamService.getTeamDetail(userId, teamId);
        return ResponseEntity.ok(response);
    }

    /**
     * 멤버 강퇴(리더 권한자만) API
     * @param leaderId
     * @param teamId
     * @param targetUserId
     * @return
     */
    @DeleteMapping("/{teamId}/users/{targetUserId}")
    public ResponseEntity<Void> kickMember(
            @AuthenticationPrincipal Long leaderId,
            @PathVariable Long teamId,
            @PathVariable Long targetUserId) {
        // 멤버 강퇴 비즈니스 로직 호출, 파라미터로 leaderId(리더 권한 유저), teamId(해당 팀 식별자), targetId(추방할 타겟 멤버)를 넘겨주었다.
        teamService.kickMember(leaderId, teamId, targetUserId);
        // 반환값이 없으니 noContent 리턴한다.
        return ResponseEntity.noContent().build();
    }

    /**
     * 팀 탈퇴 API
     * @param userId
     * @param teamId
     * @return
     */
    @DeleteMapping("/{teamId}/users/me")
    public ResponseEntity<Void> leaveTeam(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId) {
        // 팀 탈퇴 비즈니스 로직 호출, 파라미터로 userId, teamId를 넘겨줬다.
        teamService.leaveTeam(userId, teamId);
        // 반환값이 없으니 noContent 리턴한다.
        return ResponseEntity.noContent().build();
    }
}
