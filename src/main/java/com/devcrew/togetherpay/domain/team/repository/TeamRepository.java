package com.devcrew.togetherpay.domain.team.repository;

import com.devcrew.togetherpay.domain.team.Team;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    // 팀원 초대 시, 초대 코드로 팀을 찾는 메서드
    Optional<Team> findByInviteCode(String inviteCode);

    @Query("SELECT DISTINCT t FROM Team t " +
            "JOIN FETCH t.teamUsers tu " +
            "JOIN FETCH tu.user " +
            "WHERE t.id IN (SELECT tu2.team.id FROM TeamUser tu2 WHERE tu2.user.id = :userId)")
    List<Team> findAllByUserIdWithUsers(@Param("userId") Long userId);
}
