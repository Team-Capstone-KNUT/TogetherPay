package com.devcrew.togetherpay.domain.settlement.service;

import com.devcrew.togetherpay.domain.expense.Expense;
import com.devcrew.togetherpay.domain.expense.Participant;
import com.devcrew.togetherpay.domain.expense.repository.ExpenseRepository;
import com.devcrew.togetherpay.domain.settlement.Settlement;
import com.devcrew.togetherpay.domain.settlement.dto.FindDetailSettlementResponse;
import com.devcrew.togetherpay.domain.settlement.dto.FindSettlementsResponse;
import com.devcrew.togetherpay.domain.settlement.repository.SettlementRepository;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamUser;
import com.devcrew.togetherpay.domain.team.repository.TeamRepository;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
public class SettlementService {

  private final UserRepository userRepository;
  private final TeamRepository teamRepository;
  private final ExpenseRepository expenseRepository;
  private final SettlementRepository settlementRepository;

  /**
   *
   * @param userId
   * @param expenseId
   * 정산 요청
   */
  public void create(Long userId, Long expenseId) {

    Expense expense = getExpense(expenseId);

    // 지출 결제자
    Participant participant = getPayer(expense);

    // 결제자 검증
    validateUser(userId, participant);

    // 정산자 모두 (결제자 빼고)
    List<Participant> participants = expense.getParticipants().stream()
        .filter(p -> !p.isPayer())
        .toList();

    List<Settlement> settlements = participants.stream()
        .map(p -> {
          return Settlement.of(p.getKrwAmount(), expense, p.getUser());
        }).toList();

    expense.insertSettlements(settlements);

    settlementRepository.saveAll(settlements);
  }

  /**
   *
   * @param userId
   * @param settlementId
   * @return FindDetailSettlementResponse
   * 정산 상세 조회 [지출 참여자]
   */
  @Transactional(readOnly = true)
  public FindDetailSettlementResponse getSettlement(Long userId, Long settlementId) {
    User user = getUser(userId);

    Settlement settlement = settlementRepository.findByIdAndUser_Id(settlementId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

    return FindDetailSettlementResponse.of(settlement, user.getNickname());
  }

  /**
   * @param userId
   * @param teamId
   * @return FindSettlementsResponse
   * 팀 정산 목록 조회 [팀 멤버]
   */
  @Transactional(readOnly = true)
  public FindSettlementsResponse getTeamSettlements(Long userId, Long teamId) {
    // 팀 멤버 검증
    Team team = getTeam(teamId);

    validateTeamMember(userId, team.getTeamUsers());

    List<Settlement> settlements = settlementRepository.findByExpense_Team_Id(teamId);

    return FindSettlementsResponse.of(settlements);
  }

  /**
   *
   * @param userId
   * @return FindSettlementsResponse
   * 모든 정산 목록 조회 [지출 참여자]
   */
  @Transactional(readOnly = true)
  public FindSettlementsResponse getMySettlements(Long userId) {
    List<Settlement> settlements = settlementRepository.findByUser_Id(userId);
    return FindSettlementsResponse.of(settlements);
  }

  /**
   *
   * @param userId
   * @param expenseId
   * @return FindSettlementsResponse
   * 정산 목록 조회 [결제자]
   */
  @Transactional(readOnly = true)
  public FindSettlementsResponse getSettlements(Long userId, Long expenseId) {
    User user = getUser(userId);

    Expense expense = getExpense(expenseId);

    // 결제자 조회 및 검증
    Participant participant = getPayer(expense);
    validateUser(userId, participant);

    List<Settlement> settlements =
        settlementRepository.findByExpense_Id(expenseId);

    return FindSettlementsResponse.of(settlements);
  }

  private Participant getPayer(Expense expense) {
    return expense.getParticipants().stream()
        .filter(Participant::isPayer)
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYER_NOT_FOUND));
  }

  private static void validateUser(Long userId, Participant participant) {
    if(!userId.equals(participant.getUser().getId())) {
      throw new BusinessException(ErrorCode.NOT_MATCH_USER);
    }
  }

  private Expense getExpense(Long expenseId) {
    return expenseRepository.findById(expenseId)
        .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
  }

  private User getUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    return user;
  }

  private Team getTeam(Long teamId) {
    Team team = teamRepository.findById(teamId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    return team;
  }

  private void validateTeamMember(Long userId, List<TeamUser> teamUsers) {
    boolean isMember = teamUsers.stream()
        .anyMatch(tu -> tu.getUser().getId().equals(userId));
    if (!isMember) {
      throw new BusinessException(ErrorCode.NOT_A_TEAM_USER);
    }
  }


}
