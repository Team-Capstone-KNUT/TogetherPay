package com.devcrew.togetherpay.domain.settlement.repository;

import com.devcrew.togetherpay.domain.settlement.Settlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

  // 단건 상세 조회 (내가 보낼 돈이거나, 받을 돈인 정산 내역) + N+1 방지
  @Query("SELECT s FROM Settlement s " +
          "JOIN FETCH s.sender " +
          "JOIN FETCH s.receiver " +
          "WHERE s.id = :settlementId AND (s.sender.id = :userId OR s.receiver.id = :userId)")
  Optional<Settlement> findByIdAndParticipant(@Param("settlementId") Long settlementId, @Param("userId") Long userId);

  // 내 정산 목록 (내가 보낼 돈이거나 받을 돈 모두) + 지출/유저 정보 N+1 방지
  @Query("SELECT s FROM Settlement s " +
          "JOIN FETCH s.sender " +
          "JOIN FETCH s.receiver " +
          "JOIN FETCH s.expense " +
          "WHERE s.sender.id = :userId OR s.receiver.id = :userId")
  List<Settlement> findAllByParticipant(@Param("userId") Long userId);

  // 특정 지출의 정산 목록 (송금인, 수취인 모두 FETCH JOIN 하여 N+1 방지)
  @Query("SELECT s FROM Settlement s " +
          "JOIN FETCH s.sender " +
          "JOIN FETCH s.receiver " +
          "WHERE s.expense.id = :expenseId")
  List<Settlement> findByExpenseId(@Param("expenseId") Long expenseId);

  // 특정 여행(Trip)의 정산 목록 + N+1 방지
  @Query("SELECT s FROM Settlement s " +
          "JOIN FETCH s.sender " +
          "JOIN FETCH s.receiver " +
          "WHERE s.expense.trip.id = :tripId")
  List<Settlement> findByTripId(@Param("tripId") Long tripId);

  boolean existsByExpenseId(Long expenseId);
}
