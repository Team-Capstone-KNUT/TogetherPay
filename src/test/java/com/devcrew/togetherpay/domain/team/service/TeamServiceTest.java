package com.devcrew.togetherpay.domain.team.service;

import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamUser;
import com.devcrew.togetherpay.domain.team.repository.TeamRepository;
import com.devcrew.togetherpay.domain.team.repository.TeamUserRepository;
import com.devcrew.togetherpay.domain.user.Provider;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.UserRole;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamUserRepository teamUserRepository;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(teamRepository, userRepository, teamUserRepository);
    }

    @Test
    void leaderAddsMembersToExistingTeam() {
        User leader = user(1L, "leader");
        User firstMember = user(2L, "first");
        User secondMember = user(3L, "second");
        Team team = team(10L, leader);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(teamUserRepository.findByTeamAndUser(team, leader))
                .thenReturn(Optional.of(team.getTeamUsers().get(0)));
        when(userRepository.findAllById(List.of(2L, 3L))).thenReturn(List.of(firstMember, secondMember));
        when(teamUserRepository.existsAnyByTeamIdAndUserIds(10L, List.of(2L, 3L))).thenReturn(false);

        teamService.addMembers(1L, 10L, List.of(2L, 2L, 3L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TeamUser>> captor = ArgumentCaptor.forClass(List.class);
        verify(teamUserRepository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue())
                .extracting(teamUser -> teamUser.getUser().getId())
                .containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void rejectsRequestWhenAnyMemberAlreadyBelongsToTeam() {
        User leader = user(1L, "leader");
        User member = user(2L, "member");
        Team team = team(10L, leader);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(teamUserRepository.findByTeamAndUser(team, leader))
                .thenReturn(Optional.of(team.getTeamUsers().get(0)));
        when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(member));
        when(teamUserRepository.existsAnyByTeamIdAndUserIds(10L, List.of(2L))).thenReturn(true);

        assertThatThrownBy(() -> teamService.addMembers(1L, 10L, List.of(2L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_TEAM_MEMBER));
    }

    @Test
    void rejectsWholeRequestWhenAnyRequestedUserDoesNotExist() {
        User leader = user(1L, "leader");
        User existingUser = user(2L, "existing");
        Team team = team(10L, leader);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(teamUserRepository.findByTeamAndUser(team, leader))
                .thenReturn(Optional.of(team.getTeamUsers().get(0)));
        when(userRepository.findAllById(List.of(2L, 999L))).thenReturn(List.of(existingUser));

        assertThatThrownBy(() -> teamService.addMembers(1L, 10L, List.of(2L, 999L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void rejectsRequestWhenRequesterIsNotLeader() {
        User requester = user(1L, "member");
        Team team = Team.createTeam("도쿄팀", "1234");
        ReflectionTestUtils.setField(team, "id", 10L);
        TeamUser teamUser = TeamUser.createMember(team, requester);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(teamUserRepository.findByTeamAndUser(team, requester)).thenReturn(Optional.of(teamUser));

        assertThatThrownBy(() -> teamService.addMembers(1L, 10L, List.of(2L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_A_TEAM_LEADER));
    }

    @Test
    void convertsConcurrentDuplicateInsertToAlreadyMemberConflict() {
        User leader = user(1L, "leader");
        User member = user(2L, "member");
        Team team = team(10L, leader);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(teamUserRepository.findByTeamAndUser(team, leader))
                .thenReturn(Optional.of(team.getTeamUsers().get(0)));
        when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(member));
        when(teamUserRepository.existsAnyByTeamIdAndUserIds(10L, List.of(2L))).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(teamUserRepository).saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());

        assertThatThrownBy(() -> teamService.addMembers(1L, 10L, List.of(2L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_TEAM_MEMBER));
    }

    private Team team(Long id, User leader) {
        Team team = Team.createTeam("도쿄팀", "1234");
        ReflectionTestUtils.setField(team, "id", id);
        TeamUser.createLeader(team, leader);
        return team;
    }

    private User user(Long id, String nickname) {
        User user = User.builder()
                .email(nickname + "@example.com")
                .nickname(nickname)
                .role(UserRole.USER)
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(id))
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
