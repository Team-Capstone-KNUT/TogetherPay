package com.devcrew.togetherpay.domain.team.service;

import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamRole;
import com.devcrew.togetherpay.domain.team.TeamUser;
import com.devcrew.togetherpay.domain.team.dto.MemberResponse;
import com.devcrew.togetherpay.domain.team.dto.TeamDetailResponse;
import com.devcrew.togetherpay.domain.team.dto.TeamSimpleResponse;
import com.devcrew.togetherpay.domain.team.repository.TeamRepository;
import com.devcrew.togetherpay.domain.team.repository.TeamUserRepository;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamUserRepository teamUserRepository;

    /**
     * 팀 생성 비즈니스 로직
     * @param userId
     * @param name
     * @param password
     * @return
     */
    @Transactional
    public Team createTeam(Long userId, String name, String password) {
        // 유저 검증 메서드 호출
        User user = getUserOrThrow(userId);
        // 같은 유저는 같은 이름의 팀을 생성할 수 없다.
        if (teamUserRepository.existsByUserAndTeam_Name(user, name)) {
            log.warn("팀 생성 중복 시도. userId: {}, teamName: {}", userId, name);
            throw new BusinessException(ErrorCode.TEAM_NAME_ALREADY_EXISTS);
        }
        // 팀 생성 팩토리 메서드 호출(name, password)
        Team team = Team.createTeam(name, password);
        // 위에서 생성한 팀 객체와 유저 객체를 팩토리 메서드로 전달, role까지 묶어서 만들어줌
        TeamUser.createLeader(team, user);
        return teamRepository.save(team);
    }

    /**
     * 팀 가입 비즈니스 로직
     * @param userId
     * @param inviteCode
     * @param password
     */
    @Transactional
    public void joinTeam(Long userId, String inviteCode, String password) {
        // 유저 검증 메서드 호출
        User user = getUserOrThrow(userId);
        // 팀 초대 코드가 일치한지 검증
        Team team = teamRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        // 해당 팀 패스워드가 일치한지 검증 메서드 호출
        team.validatePassword(password);
        // 멤버 중복 가입 방어 로직
        if (teamUserRepository.existsByTeamAndUser(team, user)) {
            throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
        }
        // 해당 유저를 팀에 추가시키기 위해서 createMember 메서드 호출. 파라미터로 팀이랑 유저 객체 함께 넘겨준다.
        TeamUser teamUser = TeamUser.createMember(team, user);
        teamUserRepository.save(teamUser);
    }

    /**
     * 팀 삭제 비즈니스 로직(리더만)
     * @param userId
     * @param teamId
     */
    @Transactional
    public void deleteTeam(Long userId, Long teamId) {
        // 유저 검증 메서드 호출
        User user = getUserOrThrow(userId);
        // 팀 검증 메서드 호출
        Team team = getTeamOrThrow(teamId);
        // 해당 유저가 팀에 속해있는지 여부 검증하는 메서드 호츌
        TeamUser teamUser = getTeamUserOrThrow(team, user);
        // 해당 유저가 리더인지 검증하는 메서드 호출.(방장이 아닌 경우 예외 발생)
        teamUser.validateLeader();
        // 팀 삭제
        teamRepository.delete(team);
        log.info("팀 삭제 완료. teamId: {}, deleteBy(userId): {}", teamId, userId);
    }

    /**
     * 팀 이름 수정 비즈니스 로직
     * @param userId
     * @param teamId
     * @param newName
     */
    @Transactional
    public void updateTeamName(Long userId, Long teamId, String newName) {
        // 유저 검증 메서드 호출
        User user = getUserOrThrow(userId);
        // 팀 검증 메서드 호출
        Team team = getTeamOrThrow(teamId);
        // 팀 유저 검증 메서드 호출
        TeamUser teamUser = getTeamUserOrThrow(team, user);
        // 유저 권한이 리더인지 확인
        teamUser.validateLeader();
        // 엔티티에 생성해둔 팀 이름 변경 편의 메서드 호출
        team.updateName(newName);
        log.info("팀 이름 수정 완료. teamId: {}, newName: {}", teamId, newName);
    }

    /**
     * 팀 조회
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public List<TeamSimpleResponse> getMyTeams(Long userId) {

        List<TeamUser> myTeamUsers = teamUserRepository.findAllByUserIdWithTeam(userId);

        return myTeamUsers.stream()
                .map(tu -> TeamSimpleResponse.builder()
                        .teamId(tu.getTeam().getId())
                        .name(tu.getTeam().getName())
                        .myrole(tu.getRole())
                        .build()
                ).toList();
    }

    /**
     * 팀 상세 조회
     * @param userId
     * @param teamId
     * @return
     */
    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamDetail(Long userId, Long teamId) {
        Team team = getTeamOrThrow(teamId);
        User user = getUserOrThrow(userId);

        // 팀에 속한 멤버인지 권한 검증
        getTeamUserOrThrow(team, user);

        // 팀에 속한 전체 멤버 목록 조회
        List<TeamUser> teamMembers = teamUserRepository.findAllByTeamIdWithUser(teamId);

        List<MemberResponse> memberResponses = teamMembers.stream()
                .map(tu -> MemberResponse.builder()
                        .userId(tu.getUser().getId())
                        .nickname(tu.getUser().getNickname())
                        .role(tu.getRole())
                        .build())
                .toList();

        return TeamDetailResponse.builder()
                .teamId(team.getId())
                .name(team.getName())
                .inviteCode(team.getInviteCode())
                .members(memberResponses)
                .build();
    }

    /**
     * 멤버 강퇴
     * @param leaderId
     * @param teamId
     * @param targetUserId
     */
    @Transactional
    public void kickMember(Long leaderId, Long teamId, Long targetUserId) {
        // 해당 팀이 존재하는지 검증
        Team team = getTeamOrThrow(teamId);
        // 요청을 보낸 유저(leaderUser)가 존재하는지 검증
        User leader = getUserOrThrow(leaderId);
        // 강퇴할 유저(targetUser)가 존재하는지 검증
        User targetUser = getUserOrThrow(targetUserId);
        // 요청을 보낸 유저가 팀 내에 존재하는지 검증
        TeamUser leaderUser = getTeamUserOrThrow(team, leader);
        // 요청을 보낸 유저가 방장인지 권한을 검증
        leaderUser.validateLeader();

        // 방장이 자신을 강퇴하려는지를 확인한다.
        if (leaderId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_SELF);
        }

        // 강퇴할 대상 유저가 팀에 속하고 있는지 검증
        TeamUser targetTeamUser = getTeamUserOrThrow(team, targetUser);
        // 실제 삭제 부분
        teamUserRepository.delete(targetTeamUser);

        log.info("멤버 강퇴 완료. teamId: {}, leaderId: {}, kickedUserId: {}", teamId, leaderId, targetUserId);
    }

    /**
     * 팀 탈퇴
     * @param userId
     * @param teamId
     */
    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        // 해당 팀이 존재하는지 먼저 검증
        Team team = getTeamOrThrow(teamId);
        // 해당 유저가 존재하는지 검증
        User user = getUserOrThrow(userId);
        // 해당 팀 내에 유저가 속하고 있는지 검증
        TeamUser teamUser = getTeamUserOrThrow(team, user);
        // 해당 유저의 권한이 리더가 아닌 경우, 예외 발생시킨다.(방장은 바로 탈퇴 불가능)
        if (teamUser.getRole() == TeamRole.LEADER) {
            throw new BusinessException(ErrorCode.TEAM_LEADER_CANNOT_LEAVE);
        }
        // 해당 유저는 팀에서 탈퇴된다.
        teamUserRepository.delete(teamUser);
        log.info("팀 탈퇴 완료. teamId: {}, userId: {}", teamId, userId);
    }

    // 유저 검증 메서드(유저 존재여부)
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 팀 검증 메서드(팀 존재여부)
    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    // 팀 유저 검증 메서드(유저가 팀에 속해 있는지 여부 확인)
    private TeamUser getTeamUserOrThrow(Team team, User user) {
        return teamUserRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND));
    }
}
