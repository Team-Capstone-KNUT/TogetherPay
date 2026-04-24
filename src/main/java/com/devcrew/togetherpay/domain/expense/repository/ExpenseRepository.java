package com.devcrew.togetherpay.domain.expense.repository;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.expense.Expense;
import com.devcrew.togetherpay.domain.trip.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    // 여행 단위로 지출 목록을 가져온다.
    List<Expense> findByTrip_Id(Long tripId);
}
