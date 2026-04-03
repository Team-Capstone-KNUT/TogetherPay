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

    /**
     * 특정 날짜 예산 등록 비즈니스 로직
     * @param userId
     * @param teamId
     * @param budgetDate
     * @param amount
     * @return
     */
    @Transactional
    public BudgetResponse createDailyBudget(Long userId, Long teamId, LocalDate budgetDate, Long amount) {
        // 유저 검증 메서드 호출
        User user = getUserOrThrow(userId);
        // 팀 검증 메서드 호출
        Team team = getTeamOrThrow(teamId);
        // 팀 소속 여부 검증 메서드 호출
        TeamUser teamUser = getTeamUserOrThrow(team, user);
        // 권한 검증(해당 유저가 리더인지) 메서드 호출
        teamUser.validateLeader(); // 위에서 teamUser 객체를 받
        // 예산 중복 검증, 해당 날짜에 등록된 예산이 있는지 확인한다.
        if (budgetRepository.existsByTeamAndBudgetDate(team, budgetDate)) {
            log.warn("예산 중복 설정 시도. teamId: {}, date: {}", teamId, budgetDate);
            throw new BusinessException(ErrorCode.BUDGET_ALREADY_EXISTS);
        }
        // 예산 등록 메서드 호출
        Budget budget = Budget.createDailyBudget(team, budgetDate, Money.wons(amount));
        // 예산 저장 메서드 호출
        Budget savedBudget = budgetRepository.save(budget);

        log.info("일별 예산 설정 완료. budgetId: {}, date: {}, amount: {}", savedBudget.getId(), budgetDate, amount);
        // 리턴값으로 Response 포맷으로 변환하여 객체 반환한다.
        return BudgetResponse.from(savedBudget);
    }

    // 유저 검증 메서드(유저 존재 여부)
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 팀 검증 메서드(팀 존재 여부)
    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    // 팀 유저 검증 메서드(유저가 팀에 속해 있는지 여부)
    private TeamUser getTeamUserOrThrow(Team team, User user) {
        return teamUserRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_MEMBER_NOT_FOUND));
    }

}
