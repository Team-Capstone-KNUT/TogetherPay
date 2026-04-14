package com.devcrew.togetherpay.domain.budget.repository;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.trip.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    boolean existsByTripAndBudgetDate(Trip trip, LocalDate budgetDate);

    Optional<Budget> findByTripAndBudgetDate(Trip trip, LocalDate budgetDate);

    // 특정 여행 예산을 날짜순으로 조회(오름차순)
    List<Budget> findAllByTripOrderByBudgetDateAsc(Trip trip);
}
