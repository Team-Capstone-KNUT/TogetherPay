package com.devcrew.togetherpay.domain.team;

import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.global.common.BaseTimeEntity;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "team_users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamUser extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_user_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRole role;

    @Builder(access = AccessLevel.PRIVATE)
    private TeamUser(Team team, User user, TeamRole role) {
        this.team = team;
        this.user = user;
        this.role = role;
        team.addTeamUser(this);
    }

    // 팀 생성 팩토리 메서드
    public static TeamUser createLeader(Team team, User user) {
        return TeamUser.builder()
                .team(team)
                .user(user)
                .role(TeamRole.LEADER)
                .build();
    }

    // 팀 가입 팩토리 메서드
    public static TeamUser createMember(Team team, User user) {
        return TeamUser.builder()
                .team(team)
                .user(user)
                .role(TeamRole.MEMBER)
                .build();
    }

    // 권한 검증 메서드
    public void validateLeader() {
        if (this.role != TeamRole.LEADER) {
            throw new BusinessException(ErrorCode.NOT_TEAM_LEADER);
        }
    }
}
