package com.devcrew.togetherpay.domain.team.repository;

import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamUser;
import com.devcrew.togetherpay.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamUserRepository extends JpaRepository <TeamUser, Long> {
    boolean existsByTeamAndUser(Team team, User user);
}
