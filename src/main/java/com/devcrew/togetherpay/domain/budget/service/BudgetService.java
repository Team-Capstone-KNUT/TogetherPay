package com.devcrew.togetherpay.domain.budget.service;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.budget.dto.BudgetResponse;
import com.devcrew.togetherpay.domain.budget.repository.BudgetRepository;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamUser;
import com.devcrew.togetherpay.domain.team.repository.TeamRepository;
import com.devcrew.togetherpay.domain.team.repository.TeamUserRepository;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.common.vo.Money;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final TeamRepository teamRepository;
    private final TeamUserRepository teamUserRepository;
    private final UserRepository userRepository;

    @Transactional
    public BudgetResponse createDailyBudget(Long userId, Long teamId, LocalDate budgetDate, Long amount) {
        User user = getUserOrThrow(userId);
        Team team = getTeamOrThrow(teamId);
        TeamUser teamUser = getTeamUserOrThrow(team, user);

        // 권한 검증 메서드 호출
        teamUser.validateLeader();

        if (budgetRepository.existsByTeamAndBudgetDate(team, budgetDate)) {
            log.warn("예산 중복 설정 시도. teamId: {}, date: {}", teamId, budgetDate);
            throw new BusinessException(ErrorCode.BUDGET_ALREADY_EXISTS);
        }

        Budget budget = Budget.createDailyBudget(team, budgetDate, Money.wons(amount));
        Budget savedBudget = budgetRepository.save(budget);

        log.info("일별 예산 설정 완료. budgetId: {}, date: {}, amount: {}", savedBudget.getId(), budgetDate, amount);

        return BudgetResponse.from(savedBudget);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Team getTeamOrThrow(Long userId) {
        return teamRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private TeamUser getTeamUserOrThrow(Long userId) {
        return teamUserRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND));
    }

}
