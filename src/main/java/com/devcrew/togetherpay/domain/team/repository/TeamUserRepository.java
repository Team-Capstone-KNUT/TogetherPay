package com.devcrew.togetherpay.domain.team.repository;

import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamUser;
import com.devcrew.togetherpay.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamUserRepository extends JpaRepository<TeamUser, Long> {
    // 팀 가입 시의 중복 검사용 메서드
    boolean existsByTeamAndUser(Team team, User user);
    // 예산/지출 등 도메인 로직 시 특정 팀원의 정보, 권한 조회 메서드
    Optional<TeamUser> findByTeamAndUser(Team team, User user);
}
