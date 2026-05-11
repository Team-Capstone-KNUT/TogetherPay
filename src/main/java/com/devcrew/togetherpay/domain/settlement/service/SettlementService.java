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
import com.devcrew.togetherpay.domain.team.repository.TeamUserRepository;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.domain.trip.repository.TripRepository;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
public class SettlementService {

  private final UserRepository userRepository;
  private final TripRepository tripRepository;
  private final TeamUserRepository teamUserRepository;
  private final ExpenseRepository expenseRepository;
  private final SettlementRepository settlementRepository;
  @PersistenceContext private EntityManager em;

  /**
   *
   * @param userId
   * @param expenseId
   * 정산 요청
   */
  public void create(Long userId, Long expenseId) {

    Expense expense = getExpense(expenseId);

    if (settlementRepository.existsByExpenseId(expenseId)) {
      throw new BusinessException(ErrorCode.ALREADY_SETTLED);
    }

    // 지출 결제자
    Participant payerParticipant = getPayer(expense);

    // 결제자 검증
    validateUser(userId, payerParticipant);

    // 돈 받을 사람(결제자) 유저 객체 가져오기
    User receiverUser = payerParticipant.getUser();

    // 정산자 모두 (결제자 빼고)
    List<Participant> participants = expense.getParticipants().stream()
        .filter(p -> !p.isPayer())
        .toList();

    List<Settlement> settlements = participants.stream()
            .map(p -> {
              // Integer -> Long으로 변경해서 타입캐스팅
              Long amount = p.getKrwAmount() != null ? p.getKrwAmount().longValue() : 0L;
              User senderUser = p.getUser(); // 돈 보낼 사람(정산 참여자)
              return Settlement.of(amount, expense, senderUser, receiverUser); // 송금자랑 수취자로 분리
            }).toList();

    expense.addSettlements(settlements);

    settlementRepository.saveAll(settlements);
  }

  /**
   * 송금 상태 변경 (보낸 사람이 확인)
   */
  public void updateTransferStatus(Long userId, Long settlementId) {

    Settlement settlement = settlementRepository.findById(settlementId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

    // 송금자 본인인지 권한 검증
    if (!settlement.getSender().getId().equals(userId)) {
      throw new BusinessException(ErrorCode.NOT_MATCH_USER);
    }

    settlement.completeTransfer();

    Expense expense = settlement.getExpense();

    boolean isAllFinished = expense.getSettlements().stream()
            .allMatch(Settlement::isTransferred);

    if (isAllFinished) {
      expense.completeSettlement(); // isSettled = true
    }
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

    Settlement settlement = settlementRepository.findByIdAndParticipant(settlementId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

    return FindDetailSettlementResponse.of(settlement, userId);
  }

  /**
   * @param userId
   * @param tripId
   * @return FindSettlementsResponse
   * 특정 여행의 정산 목록 조회
   */
  @Transactional(readOnly = true)
  public FindSettlementsResponse getTripSettlements(Long userId, Long tripId) {
    // 여행 조회
    Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND)); // 에러코드 필요

    // 팀 멤버 검증
    validateUserIsTeamMember(userId, trip.getTeam());

    List<Settlement> settlements = settlementRepository.findByTripId(tripId);

    return FindSettlementsResponse.of(settlements, userId);
  }

  /**
   *
   * @param userId
   * @return FindSettlementsResponse
   * 모든 정산 목록 조회 [지출 참여자]
   */
  @Transactional(readOnly = true)
  public FindSettlementsResponse getMySettlements(Long userId) {
    List<Settlement> settlements = settlementRepository.findAllByParticipant(userId);
    return FindSettlementsResponse.of(settlements, userId);
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
        settlementRepository.findByExpenseId(expenseId);

    return FindSettlementsResponse.of(settlements, userId);
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

  private void validateUserIsTeamMember(Long userId, Team team) {
    User user = getUser(userId);
    if (!teamUserRepository.existsByTeamAndUser(team, user)) {
      throw new BusinessException(ErrorCode.NOT_A_TEAM_USER);
    }
  }


}
