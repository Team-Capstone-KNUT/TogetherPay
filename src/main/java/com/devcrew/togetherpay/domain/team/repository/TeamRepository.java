package com.devcrew.togetherpay.domain.team.repository;

import com.devcrew.togetherpay.domain.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    // 팀원 초대 시, 초대 코드로 팀을 찾는 메서드
    Optional<Team> findByInviteCode(String inviteCode);

}
