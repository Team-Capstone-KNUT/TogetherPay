package com.devcrew.togetherpay.domain.team;

import com.devcrew.togetherpay.global.common.BaseTimeEntity;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "teams")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 8)
    private String inviteCode;

    @Column(nullable = false)
    private String password;

    // 양방향 매핑: 팀이 삭제되면 속한 팀원 정보(TeamUser)도 함께 날아가도록 Cascade 설정
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamUser> teamUsers = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    // 팩토리 메서드로만 생성하도록 제한(빌더에 액세스 레벨 PRIVATE로)
    @Builder(access = AccessLevel.PRIVATE)
    private Team(String name, String password) {
        this.name = name;
        this.password = password;
        this.inviteCode = generateInviteCode();
    }

    // 팀 생성 팩토리 메서드
    public static Team createTeam(String name, String password) {
        return Team.builder()
                .name(name)
                .password(password)
                .build();
    }

    // 팀 비밀번호 검증 메서드
    public void validatePassword(String rawPassword) {
        if (!this.password.equals(rawPassword)) {
            throw new BusinessException(ErrorCode.INVALID_TEAM_PASSWORD);
        }
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void addTeamUser(TeamUser teamUser) {
        this.teamUsers.add(teamUser);
    }
}
