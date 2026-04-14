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

  // 특정 지출의 정산 목록 (유저 정보 N+1 방지)
  @Query("SELECT s FROM Settlement s JOIN FETCH s.user WHERE s.expense.id = :expenseId")
  List<Settlement> findByExpense_Id(@Param("expenseId") Long expenseId);

  // 내 정산 목록 (지출 정보와 유저 정보를 한 번에 가져와 N+1 방지)
  @Query("SELECT s FROM Settlement s JOIN FETCH s.user JOIN FETCH s.expense WHERE s.user.id = :userId")
  List<Settlement> findByUser_Id(@Param("userId") Long userId);

  // 특정 여행(Trip)의 정산 목록 (기존 Team -> Trip 으로 변경 + N+1 방지)
  @Query("SELECT s FROM Settlement s JOIN FETCH s.user WHERE s.expense.trip.id = :tripId")
  List<Settlement> findByExpense_Trip_Id(@Param("tripId") Long tripId);

  Optional<Settlement> findByIdAndUser_Id(Long id, Long userId);
}
