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
     * 팀 개설 및 방장(Leader) 지정
     */
    @Transactional
    public Team createTeam(Long userId, String name, String password) {
        User user = getUserOrThrow(userId);

        Team team = Team.createTeam(name, password);
        TeamUser.createLeader(team, user);

        return teamRepository.save(team);
    }

    @Transactional
    public void joinTeam(Long userId, String inviteCode, String password) {
        User user = getUserOrThrow(userId);
        Team team = teamRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        team.validatePassword(password);

        // 멤버 중복 가입 방어 로직
        if (teamUserRepository.existsByTeamAndUser(team, user)) {
            throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
        }

        TeamUser teamUser = TeamUser.createMember(team, user);
        teamUserRepository.save(teamUser);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
