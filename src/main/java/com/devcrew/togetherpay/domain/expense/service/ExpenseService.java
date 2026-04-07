package com.devcrew.togetherpay.domain.expense.service;

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
import com.devcrew.togetherpay.domain.team.TeamUser;
import com.devcrew.togetherpay.domain.team.repository.TeamRepository;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.common.ExchangeRate.service.ExchangeRateService;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

  private final TeamRepository teamRepository;
  private final UserRepository userRepository;
  private final ExpenseRepository expenseRepository;
  private final ExchangeRateService exchangeRateService;

  /**
   *
   * @param userId
   * @param command
   * 지출 등록 (더치페이) n 빵
   */
  public void registerWithDutchPay(Long userId, RegisterDutchExpenseCommand command) {

    // 선택한 팀 조회
    Team selectedTeam = findByTeamId(command.teamId());

    List<ParticipantInfo> participantInfos = command.participantInfos();

    // 선택한 팀에 참여자가 모두 속하는지 검증
    validateAllAreMember(selectedTeam, participantInfos);

    // 결제자 1명 검증
    validateSinglePayer(participantInfos);

    BigDecimal divideAmount = calculateDutchAmount(participantInfos, command.totalAmount());

    // 결제한 날짜 환율 조회
    BigDecimal exchangeRate = exchangeRateService.getExchangeRate(command.currency(), command.expenseDate());

    Expense expense = command.toEntity(selectedTeam, command.totalAmount(), exchangeRate);

    expense.calculateKRW();

    // 참여자 할당
    assignParticipants(expense, participantInfos, p -> divideAmount);

    expenseRepository.save(expense);
  }

  /**
   *
   * @param userId
   * @param command
   * 지출 등록 (개별 금액)
   */
  public void registerWithIndividualAmount(Long userId, RegisterIndividualExpenseCommand command) {

    // 선택한 팀 조회
    Team selectedTeam = findByTeamId(command.teamId());

    List<ParticipantInfo> participantInfos = command.participantInfos();

    // 선택한 팀에 참여자가 모두 속하는지 검증
    validateAllAreMember(selectedTeam, participantInfos);

    // 결제자 1명 검증
    validateSinglePayer(participantInfos);

    BigDecimal totalAmount = calculateIndividualAmount(participantInfos);

    // 지출 등록 시점 환율 조회
    BigDecimal exchangeRate = exchangeRateService.getExchangeRate(command.currency(), command.expenseDate());

    Expense expense = command.toEntity(selectedTeam, totalAmount, exchangeRate);

    expense.calculateKRW();

    // 참여자 할당
    assignParticipants(expense, participantInfos, ParticipantInfo::amount);

    expenseRepository.save(expense);

  }


  /**
   *
   * @param userId
   * @param expenseId
   * @return findExpenseResponse
   * 지출 상세 조회
   */
  @Transactional(readOnly = true)
  public FindDetailExpenseResponse getExpense(Long userId, Long expenseId) {
    Expense expense = expenseRepository.findById(expenseId)
        .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));

    // 속한 팀 조회
    Team team = expense.getTeam();

    // 팀에 속한 멤버들 조회
    List<TeamUser> teamUsers = team.getTeamUsers();

    invalidateTeamUser(userId, teamUsers);

    return FindDetailExpenseResponse.from(expense);
  }


  /**
   *
   * @param userId
   * @param teamId
   * @return findExpensesResponse
   * 지출 목록 조회
   */
  @Transactional(readOnly = true)
  public FindExpensesResponse getExpenses(Long userId, Long teamId) {
    Team team = findByTeamId(teamId);

    List<TeamUser> teamUsers = team.getTeamUsers();

    invalidateTeamUser(userId, teamUsers);

    List<Expense> expenses = team.getExpenses();

    return FindExpensesResponse.from(expenses);
  }


  /**
   *
   * @param userId
   * @param expenseId
   * @param command
   * 지출 수정
   */
  public void updateExpense(Long userId, Long expenseId, UpdateExpenseCommand command) {
    Expense expense = expenseRepository.findById(expenseId)
        .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));

    // 속한 팀 조회
    Team team = expense.getTeam();
    // 팀에 속한 멤버들 조회
    List<TeamUser> teamUsers = team.getTeamUsers();
    invalidateTeamUser(userId, teamUsers);

    expense.clearParticipants();

    if (command.isDutchPay() == true && command.totalAmount() != null) {
      updateDutchPay(expense, command);
    } else {
      updateIndividualAmount(expense, command);
    }

  }

  /**
   *
   * @param userId
   * @param expenseId
   * 지출 삭제
   */
  public void deleteExpense(Long userId, Long expenseId) {
    Expense expense = expenseRepository.findById(expenseId)
        .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));

    // 속한 팀 조회
    Team team = expense.getTeam();

    // 팀에 속한 멤버들 조회
    List<TeamUser> teamUsers = team.getTeamUsers();

    invalidateTeamUser(userId, teamUsers);

    expenseRepository.delete(expense);
  }

  private Team findByTeamId(Long teamId) {
    return teamRepository.findById(teamId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
  }

  private void validateAllAreMember(Team team, List<ParticipantInfo> participantInfos) {
    List<TeamUser> teamUsers = team.getTeamUsers();

    Set<Long> teamUserIds = teamUsers.stream()
        .map(teamUser -> teamUser.getUser().getId())
        .collect(Collectors.toSet());

    List<Long> participantUserIds = participantInfos.stream()
        .map(ParticipantInfo::userId)
        .toList();

    boolean allAreTeamUser =
        teamUserIds.containsAll(participantUserIds);

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

  private void invalidateTeamUser(Long userId, List<TeamUser> teamUsers) {
    boolean isUserTeam = teamUsers.stream()
        .anyMatch(teamUser -> teamUser.getUser().getId().equals(userId));

    if (!isUserTeam) {
      throw new BusinessException(ErrorCode.NOT_A_TEAM_USER);
    }
  }

  private BigDecimal calculateIndividualAmount(List<ParticipantInfo> participantInfos) {
    return participantInfos.stream()
        .map(ParticipantInfo::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal calculateDutchAmount(List<ParticipantInfo> participantInfos, BigDecimal totalAmount) {
    int count = participantInfos.size();

    return totalAmount.divide(
        BigDecimal.valueOf(count),
        2,
        RoundingMode.HALF_UP
    );
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
    BigDecimal divideAmount =
        calculateDutchAmount(command.participantInfos(), command.totalAmount());

    BigDecimal exchangeRate = exchangeRateService.getExchangeRate(command.currency(), command.expenseDate());

    expense.updateInfo(command.title(), command.description(),
        command.currency(), command.category(), command.expenseDate(), command.method(), command.totalAmount(), exchangeRate);
    assignParticipants(expense, command.participantInfos(), p -> divideAmount);
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
