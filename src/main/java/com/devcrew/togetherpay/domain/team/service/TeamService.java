package com.devcrew.togetherpay.domain.team.service;

import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamRole;
import com.devcrew.togetherpay.domain.team.TeamUser;
import com.devcrew.togetherpay.domain.team.dto.FindTeamsResponse;
import com.devcrew.togetherpay.domain.team.dto.MemberResponse;
import com.devcrew.togetherpay.domain.team.dto.TeamDetailResponse;
import com.devcrew.togetherpay.domain.team.dto.TeamSimpleResponse;
import com.devcrew.togetherpay.domain.team.repository.TeamRepository;
import com.devcrew.togetherpay.domain.team.repository.TeamUserRepository;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
    @PersistenceContext private EntityManager em;

    /**
     * 팀 생성 비즈니스 로직
     * @param leaderId
     * @param name
     * @param password
     * @return
     */
    @Transactional
    public Team createTeam(Long leaderId, String name, String password, List<Long> memberIds) {
        // 유저 검증 메서드 호출
        User leader = getUserOrThrow(leaderId);

        // 개인 단위의 팀 이름 중복 검사, 같은 유저가 중복되는 이름의 팀을 생성할 수 없다.
        if (teamUserRepository.existsByUserAndTeam_Name(leader, name)) {
            throw new BusinessException(ErrorCode.TEAM_NAME_ALREADY_EXISTS);
        }

        // 팀 생성 팩토리 메서드 호출(name, password)
        Team team = Team.createTeam(name, password);
        // 위에서 생성한 팀 객체와 유저 객체를 팩토리 메서드로 전달, role까지 묶어서 만들어줌
        TeamUser.createLeader(team, leader);

        // 추가 멤버 일괄 초대 로직
        if (memberIds != null && !memberIds.isEmpty()) {

            // 방장은 이미 추가되었으니 제외하고 조회함.
            List<Long> filteredMemberIds = memberIds.stream()
                    .filter(id -> !id.equals(leaderId))
                    .toList();
            List<User> invitees = userRepository.findAllById(filteredMemberIds);

            // 요청한 ID 갯수와 실제 조회된 유저 수가 다른 경우 예외가 발생한다.
            if (invitees.size() != filteredMemberIds.size()) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }

            // 초대 유저들을 일반 멤버 권한으로 일괄 가입 처리
            invitees.forEach(invitee -> TeamUser.createMember(team, invitee));
        }
        Team savedTeam = teamRepository.save(team);

        /**
         * 영속성 컨텍스트 동기화
         * flush()로 INSERT 쿼리를 DB에 즉시 날리고, clear()로 1차 캐시를 비운다.
         * 이렇게 해야 이후 조회 시 DB에서 '진짜' 저장된 데이터(Team + TeamUsers)를 읽어올 수 있음.
         */
        em.flush();
        em.clear();

        return teamRepository.findById(savedTeam.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
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
    public FindTeamsResponse getMyTeams(Long userId) {

        // 팀 전체 조회(내가 속한 팀 기준으로 그 팀의 모든 유저 한번에 조회)
        List<Team> myTeams = teamRepository.findAllByUserIdWithUsers(userId);

        // DTO 변환시 userId도 함께 넘겨줌
        return FindTeamsResponse.of(myTeams, userId);
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
     * 기존 팀에 멤버 일괄 추가(리더만)
     */
    @Transactional
    public void addMembers(Long leaderId, Long teamId, List<Long> memberIds) {
        Team team = getTeamOrThrow(teamId);
        User leader = getUserOrThrow(leaderId);
        getTeamUserOrThrow(team, leader).validateLeader();

        List<Long> distinctMemberIds = memberIds.stream().distinct().toList();
        List<User> members = userRepository.findAllById(distinctMemberIds);

        if (members.size() != distinctMemberIds.size()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (teamUserRepository.existsAnyByTeamIdAndUserIds(teamId, distinctMemberIds)) {
            throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
        }

        List<TeamUser> teamUsers = members.stream()
                .map(member -> TeamUser.createMember(team, member))
                .toList();
        try {
            teamUserRepository.saveAllAndFlush(teamUsers);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
        }

        log.info("팀 멤버 추가 완료. teamId: {}, leaderId: {}, memberCount: {}",
                teamId, leaderId, teamUsers.size());
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
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_TEAM_USER));
    }
}
