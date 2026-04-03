package com.devcrew.togetherpay.domain.team.service;

import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamRole;
import com.devcrew.togetherpay.domain.team.TeamUser;
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
