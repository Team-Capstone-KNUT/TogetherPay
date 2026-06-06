package com.devcrew.togetherpay.domain.expense.repository;

import com.devcrew.togetherpay.domain.expense.Expense;
import com.devcrew.togetherpay.domain.expense.dto.ExpenseCategoryTotal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    // 여행 단위로 지출 목록을 가져온다.
    List<Expense> findByTrip_Id(Long tripId);

    @Query("select distinct e from Expense e " +
            "join fetch e.participants p " +
            "join fetch p.user " +
            "where e.trip.id = :tripId")
    List<Expense> findAllByTripIdWithParticipants(@Param("tripId") Long tripId);

    @Query("select coalesce(sum(e.totalAmount.amount), 0) from Expense e where e.trip.id = :tripId")
    BigDecimal sumTotalAmountByTripId(@Param("tripId") Long tripId);

    @Query("select new com.devcrew.togetherpay.domain.expense.dto.ExpenseCategoryTotal(e.category, coalesce(sum(e.totalAmount.amount), 0)) " +
            "from Expense e " +
            "where e.trip.id = :tripId " +
            "group by e.category " +
            "order by coalesce(sum(e.totalAmount.amount), 0) desc")
    List<ExpenseCategoryTotal> sumTotalAmountByTripIdGroupByCategory(@Param("tripId") Long tripId);
}
