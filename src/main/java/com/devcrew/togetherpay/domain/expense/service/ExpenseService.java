package com.devcrew.togetherpay.domain.expense.service;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.budget.repository.BudgetRepository;
import com.devcrew.togetherpay.domain.expense.Expense;
import com.devcrew.togetherpay.domain.expense.Participant;
import com.devcrew.togetherpay.domain.expense.controller.command.RegisterDutchExpenseCommand;
import com.devcrew.togetherpay.domain.expense.controller.command.RegisterIndividualExpenseCommand;
import com.devcrew.togetherpay.domain.expense.controller.command.UpdateExpenseCommand;
import com.devcrew.togetherpay.domain.expense.dto.FindDetailExpenseResponse;
import com.devcrew.togetherpay.domain.expense.dto.FindExpensesResponse;
import com.devcrew.togetherpay.domain.expense.dto.ParticipantInfo;
import com.devcrew.togetherpay.domain.expense.repository.ExpenseRepository;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.repository.TeamUserRepository;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.domain.trip.repository.TripRepository;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.common.ExchangeRate.service.ExchangeRateService;
import com.devcrew.togetherpay.global.common.vo.Money;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
public class ExpenseService {

  private final TripRepository tripRepository;
  private final TeamUserRepository teamUserRepository;
  private final UserRepository userRepository;
  private final ExpenseRepository expenseRepository;
  private final ExchangeRateService exchangeRateService;
  private final BudgetRepository budgetRepository;

  /**
   * 지출 등록 (더치페이) n 빵
   */
  public void registerWithDutchPay(Long userId, RegisterDutchExpenseCommand command) {

    Trip trip = getTripOrThrow(command.tripId());
    Team team = trip.getTeam();

    trip.validateDate(command.expenseDate());

    List<ParticipantInfo> participantInfos = command.participantInfos();

    validateUserIsTeamMember(userId, team);
    validateAllAreMember(team, participantInfos);
    validateSinglePayer(participantInfos);

    BigDecimal[] divAndRem = calculateDutchAmount(participantInfos, command.totalAmount());
    BigDecimal exchangeRate = exchangeRateService.getExchangeRate(command.currency(), command.expenseDate());

    Expense expense = command.toEntity(trip, command.totalAmount(), exchangeRate);
    expense.calculateKRW();

    BigDecimal baseAmount = divAndRem[0]; // 몫
    BigDecimal remainder = divAndRem[1]; // 나머지

    assignParticipants(expense, participantInfos, p -> {
      if (p.isPayer()) {
        return baseAmount.add(remainder);
      }
      return baseAmount;
    });

    expenseRepository.save(expense);

    // 예산 차감
    spendBudget(trip, command.expenseDate(), command.totalAmount());
  }

  /**
   * 지출 등록 (개별 금액)
   */
  public void registerWithIndividualAmount(Long userId, RegisterIndividualExpenseCommand command) {

    Trip trip = getTripOrThrow(command.tripId());
    Team team = trip.getTeam();

    trip.validateDate(command.expenseDate());

    List<ParticipantInfo> participantInfos = command.participantInfos();

    validateUserIsTeamMember(userId, team);
    validateAllAreMember(team, participantInfos);
    validateSinglePayer(participantInfos);

    BigDecimal totalAmount = calculateIndividualAmount(participantInfos);
    BigDecimal exchangeRate = exchangeRateService.getExchangeRate(command.currency(), command.expenseDate());

    Expense expense = command.toEntity(trip, totalAmount, exchangeRate);
    expense.calculateKRW();

    assignParticipants(expense, participantInfos, ParticipantInfo::amount);

    expenseRepository.save(expense);

    // 예산 차감
    spendBudget(trip, command.expenseDate(), totalAmount);
  }

  /**
   * 지출 상세 조회
   */
  @Transactional(readOnly = true)
  public FindDetailExpenseResponse getExpense(Long userId, Long expenseId) {
    Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));

    Team team = expense.getTrip().getTeam();
    validateUserIsTeamMember(userId, team);

    return FindDetailExpenseResponse.from(expense);
  }

  /**
   * 특정 여행의 지출 목록 조회
   */
  @Transactional(readOnly = true)
  public FindExpensesResponse getExpenses(Long userId, Long tripId) {
    Trip trip = getTripOrThrow(tripId);
    Team team = trip.getTeam();

    validateUserIsTeamMember(userId, team);

    List<Expense> expenses = expenseRepository.findByTrip_Id(tripId);

    return FindExpensesResponse.from(expenses);
  }

  /**
   * 지출 수정
   */
  public void updateExpense(Long userId, Long expenseId, UpdateExpenseCommand command) {
    Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));

    Trip trip = expense.getTrip();
    Team team = trip.getTeam();

    validateUserIsTeamMember(userId, team);
    trip.validateDate(command.expenseDate());

    // 기존 금액을 예산에 환불(복구)
    refundBudget(trip, expense.getExpenseDate(), expense.getTotalAmount());

    expense.clearParticipants();

    BigDecimal newTotalAmount;
    if (command.isDutchPay() != null && command.isDutchPay() && command.totalAmount() != null) {
      updateDutchPay(expense, command);
      newTotalAmount = command.totalAmount();
    } else {
      updateIndividualAmount(expense, command);
      newTotalAmount = calculateIndividualAmount(command.participantInfos());
    }

    // 새로운 날짜/금액으로 예산 다시 차감
    spendBudget(trip, command.expenseDate(), newTotalAmount);
  }

  /**
   * 지출 삭제
   */
  public void deleteExpense(Long userId, Long expenseId) {
    Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));

    Team team = expense.getTrip().getTeam();

    validateUserIsTeamMember(userId, team);

    // 지출 삭제 시, 기존 지출 금액을 예산에 환불(복구)
    refundBudget(expense.getTrip(), expense.getExpenseDate(), expense.getTotalAmount());

    expenseRepository.delete(expense);
  }

  private void spendBudget(Trip trip, LocalDate date, BigDecimal amount) {
    Budget budget = budgetRepository.findByTripAndBudgetDate(trip, date)
            .orElseThrow(() -> new BusinessException(ErrorCode.BUDGET_NOT_FOUND));
    budget.spend(Money.of(amount));
  }

  private void refundBudget(Trip trip, LocalDate date, BigDecimal amount) {
    Budget budget = budgetRepository.findByTripAndBudgetDate(trip, date)
            .orElseThrow(() -> new BusinessException(ErrorCode.BUDGET_NOT_FOUND));
    budget.refund(Money.of(amount));
  }

  private Trip getTripOrThrow(Long tripId) {
    return tripRepository.findById(tripId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
  }

  private User getUserOrThrow(Long userId) {
    return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }

  private void validateUserIsTeamMember(Long userId, Team team) {
    User user = getUserOrThrow(userId);

    if (!teamUserRepository.existsByTeamAndUser(team, user)) {
      throw new BusinessException(ErrorCode.NOT_A_TEAM_USER);
    }
  }

  private void validateAllAreMember(Team team, List<ParticipantInfo> participantInfos) {
    Set<Long> teamUserIds = team.getTeamUsers().stream()
            .map(teamUser -> teamUser.getUser().getId())
            .collect(Collectors.toSet());

    List<Long> participantUserIds = participantInfos.stream()
            .map(ParticipantInfo::userId)
            .toList();

    boolean allAreTeamUser = teamUserIds.containsAll(participantUserIds);

    if (!allAreTeamUser) {
      throw new BusinessException(ErrorCode.NOT_A_TEAM_USER);
    }
  }

  private void validateSinglePayer(List<ParticipantInfo> participantInfos) {
    List<ParticipantInfo> payers = participantInfos.stream()
            .filter(ParticipantInfo::isPayer)
            .toList();

    if (payers.size() != 1) {
      throw new BusinessException(ErrorCode.INVALID_PAYER_COUNT);
    }
  }

  private BigDecimal calculateIndividualAmount(List<ParticipantInfo> participantInfos) {
    return participantInfos.stream()
            .map(ParticipantInfo::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal[] calculateDutchAmount(List<ParticipantInfo> participantInfos, BigDecimal totalAmount) {
    int count = participantInfos.size();
    return totalAmount.divideAndRemainder(BigDecimal.valueOf(count));
  }

  private void assignParticipants(Expense expense, List<ParticipantInfo> participantInfos, Function<ParticipantInfo, BigDecimal> amountProvider) {
    List<Long> participantUserIds = participantInfos.stream()
            .map(ParticipantInfo::userId)
            .toList();

    Map<Long, User> userMap = userRepository.findAllById(participantUserIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

    participantInfos.forEach(p -> {
      Participant participant = Participant.of(expense, userMap.get(p.userId()), p.isPayer(), amountProvider.apply(p));
      participant.calculateKRW(expense.getExchangeRate());
      expense.addParticipant(participant);
    });
  }

  private void updateDutchPay(Expense expense, UpdateExpenseCommand command) {
    BigDecimal[] divAndRem =
            calculateDutchAmount(command.participantInfos(), command.totalAmount());

    BigDecimal exchangeRate = exchangeRateService.getExchangeRate(command.currency(), command.expenseDate());

    expense.updateInfo(command.title(), command.description(),
            command.currency(), command.category(), command.expenseDate(), command.method(), command.totalAmount(), exchangeRate);

    BigDecimal baseAmount = divAndRem[0];
    BigDecimal remainder = divAndRem[1];

    assignParticipants(expense, command.participantInfos(), p -> {
      if (p.isPayer()) {
        return baseAmount.add(remainder);
      }
      return baseAmount;
    });
  }

  private void updateIndividualAmount(Expense expense, UpdateExpenseCommand command) {
    BigDecimal totalAmount =
            calculateIndividualAmount(command.participantInfos());

    BigDecimal exchangeRate = exchangeRateService.getExchangeRate(command.currency(), command.expenseDate());

    expense.updateInfo(command.title(), command.description(),
            command.currency(), command.category(), command.expenseDate(), command.method(), totalAmount, exchangeRate);

    assignParticipants(expense, command.participantInfos(), ParticipantInfo::amount);
  }
}