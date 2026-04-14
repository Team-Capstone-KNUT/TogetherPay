package com.devcrew.togetherpay.domain.budget.service;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.budget.dto.BudgetResponse;
import com.devcrew.togetherpay.domain.budget.repository.BudgetRepository;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamRole;
import com.devcrew.togetherpay.domain.team.repository.TeamUserRepository;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.domain.trip.repository.TripRepository;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.common.vo.Money;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TripRepository tripRepository;
    private final TeamUserRepository teamUserRepository;
    private final UserRepository userRepository;

    /**
     * 예산 수정
     * @param userId
     * @param budgetId
     * @param newAmount
     * @return
     */
    @Transactional
    public BudgetResponse updateBudget(Long userId, Long budgetId, Long newAmount) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUDGET_NOT_FOUND));

        // 예산 -> 여행 -> 팀 순으로 접근
        Trip trip = budget.getTrip();
        Team team = trip.getTeam();
        User user = getUserOrThrow(userId);

        // 권한 검증(해당 유저가 팀의 리더인가?)
        teamUserRepository.findByTeamAndUser(team, user)
                .filter(teamUser -> teamUser.getRole() == TeamRole.LEADER)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_TEAM_LEADER));

        // 금액 업데이트
        budget.updateAmount(Money.wons(newAmount));

        return BudgetResponse.from(budget);
    }

    /**
     * 예산 조회(팀 멤버만 가능함)
     * @param userId
     * @param tripId
     * @return
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByTrip(Long userId, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
        Team team = trip.getTeam();

        // 권한 검증(팀 멤버인지 확인)
        validateUserIsTeamMember(userId, team);

        // 해당 여행의 예산 목록을 날짜 오름차순으로 조회
        List<Budget> budgets = budgetRepository.findAllByTripOrderByBudgetDateAsc(trip);

        return budgets.stream()
                .map(BudgetResponse::from)
                .toList();
    }

    // 유저 검증 메서드
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 팀 멤버 검증 메서드
    private void validateUserIsTeamMember(Long userId, Team team) {
        User user = getUserOrThrow(userId);
        if (!teamUserRepository.existsByTeamAndUser(team, user)) {
            throw new BusinessException(ErrorCode.NOT_A_TEAM_USER);
        }
    }
}
