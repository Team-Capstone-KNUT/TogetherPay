package com.devcrew.togetherpay.domain.budget.repository;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    // 특정 팀의 특정 날짜에 이미 등록된 예산이 있는지 여부 확인(중복 검사)
    boolean existsByTeamAndBudgetDate(Team team, LocalDate budgetDate);
}
