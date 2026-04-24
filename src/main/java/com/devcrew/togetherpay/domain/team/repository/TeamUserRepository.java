package com.devcrew.togetherpay.domain.team.repository;

import org.springframework.data.repository.query.Param;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamUser;
import com.devcrew.togetherpay.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeamUserRepository extends JpaRepository<TeamUser, Long> {

    // 팀 가입 시의 중복 검사용 메서드
    boolean existsByTeamAndUser(Team team, User user);

    // 예산/지출 등 도메인 로직 시 특정 팀원의 정보, 권한 조회 메서드
    Optional<TeamUser> findByTeamAndUser(Team team, User user);

    // 내가 속한 팀에 특정 입력한 이름과 일치하는 팀이 있는지 확인
    boolean existsByUserAndTeam_Name(User user, String teamName);

    // 내 팀 목록 조회 시 n+1 문제 방지용 fetch join
    @Query("SELECT tu FROM TeamUser tu JOIN FETCH tu.team WHERE tu.user.id = :userId")
    List<TeamUser> findAllByUserIdWithTeam(@Param("userId") Long userId);

    // 팀 상세 조회용 멤버 목록 가져오는 fetch join
    @Query("SELECT tu FROM TeamUser tu JOIN FETCH tu.user WHERE tu.team.id = :teamId")
    List<TeamUser> findAllByTeamIdWithUser(@Param("teamId") Long teamId);
}
