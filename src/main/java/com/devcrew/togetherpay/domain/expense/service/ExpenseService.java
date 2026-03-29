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

  private final ExpenseRepository expenseRepository;
  private final TeamRepository teamRepository;
  private final UserRepository userRepository;

  /**
   *
   * @param userId
   * @param command
   * 지출 등록 (더치페이) n 빵
   */
  public void registerWithDutchPay(Long userId, RegisterDutchExpenseCommand command) {

    // 선택한 팀 조회
    Team selectedTeam = teamRepository.findById(command.teamId())
        .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

    List<ParticipantInfo> participantInfos = command.participantInfos();

    // 선택한 팀에 참여자가 모두 속하는지 검증
    validateAllAreMember(selectedTeam, participantInfos);

    // 결제자 1명 검증
    validateSinglePayer(participantInfos);

    BigDecimal divideAmount = calculateDutchAmount(participantInfos, command.totalAmount());

    Expense expense = command.toEntity(selectedTeam, command.totalAmount());

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
    Team selectedTeam = teamRepository.findById(command.teamId())
        .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

    List<ParticipantInfo> participantInfos = command.participantInfos();

    // 선택한 팀에 참여자가 모두 속하는지 검증
    validateAllAreMember(selectedTeam, participantInfos);

    // 결제자 1명 검증
    validateSinglePayer(participantInfos);

    BigDecimal totalAmount = calculateIndividualAmount(participantInfos);

    Expense expense = command.toEntity(selectedTeam, totalAmount);

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
    List<TeamMember> teamMembers = team.getTeamMembers();

    invalidateTeamMember(userId, teamMembers);

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
    Team team = teamRepository.findById(teamId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

    List<TeamMember> teamMembers = team.getMembers();

    invalidateTeamMember(userId, teamMembers);

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
    List<TeamMember> teamMembers = team.getTeamMembers();
    invalidateTeamMember(userId, teamMembers);

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
    List<TeamMember> teamMembers = team.getTeamMembers();

    invalidateTeamMember(userId, teamMembers);

    expenseRepository.delete(expense);
  }

  private void validateAllAreMember(Team team, List<ParticipantInfo> participantInfos) {
    // 팀에 속한 멤버 조회
    List<TeamMember> teamMembers = team.getTeamMembers();

    // List 보다 set이 성능이 더 좋다!
    // List는 앞에서부터 하나씩 순차 탐색 [1, 2, 3, 4, 5, 6..] 찾을 떄까지 쭉 순회한다.
    // HashSet은 contains() 호출 시 hash 함수로 바로 위치 계산.
    Set<Long> teamMemberIds = teamMembers.stream()
        .map(TeamMember::getUserId)
        .collect(Collectors.toSet());

    List<Long> participantUserIds = participantInfos.stream()
        .map(ParticipantInfo::userId)
        .toList();

    // 팀 멤버에 참여자 모두가 속하는지.
    boolean allAreMember =
        teamMemberIds.containsAll(participantUserIds);

    if (!allAreMember) {
      throw new BusinessException(ErrorCode.NOT_A_TEAM_MEMBER);
    }
  }

  private void validateSinglePayer(List<ParticipantInfo> participantInfos) {
    List<ParticipantInfo> payers = participantInfos.stream()
        .filter(ParticipantInfo::isPayer)
        .toList();

    // 결제자가 2명 이상이면 예외 처리
    if (payers.size() != 1) {
      throw new BusinessException(ErrorCode.INVALID_PAYER_COUNT);
    }

  }

  private void invalidateTeamMember(Long userId, List<TeamMember> teamMembers) {
    boolean isUserTeam = teamMembers.stream()
        .anyMatch(member -> member.getUser().getId() == userId);

    if (!isUserTeam) {
      throw new BusinessException(ErrorCode.NOT_A_TEAM_MEMBER);
    }
  }

  private BigDecimal calculateIndividualAmount(List<ParticipantInfo> participantInfos) {
    BigDecimal totalAmount = participantInfos.stream()
        .map(ParticipantInfo::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add); // 초기값 0 부터 모두 더하기.

    return totalAmount;
  }

  private BigDecimal calculateDutchAmount(List<ParticipantInfo> participantInfos, BigDecimal totalAmount) {
    int count = participantInfos.size();

    return totalAmount.divide(
        BigDecimal.valueOf(count),
        2, // 소수점 2자리.
        RoundingMode.HALF_UP // 반올림
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
      expense.addParticipant(participant);
    });
  }

  private void updateDutchPay(Expense expense, UpdateExpenseCommand command) {
    BigDecimal divideAmount =
        calculateDutchAmount(command.participantInfos(), command.totalAmount());

    expense.updateInfo(command.title(), command.description(),
        command.currency(), command.category(), command.method(), command.totalAmount());

    assignParticipants(expense, command.participantInfos(), p -> divideAmount);
  }

  private void updateIndividualAmount(Expense expense, UpdateExpenseCommand command) {
    BigDecimal totalAmount =
        calculateIndividualAmount(command.participantInfos());

    expense.updateInfo(command.title(), command.description(),
        command.currency(), command.category(), command.method(), totalAmount);

    assignParticipants(expense, command.participantInfos(), ParticipantInfo::amount);
  }


}
