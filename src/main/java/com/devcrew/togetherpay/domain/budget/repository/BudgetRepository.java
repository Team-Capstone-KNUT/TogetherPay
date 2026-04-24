package com.devcrew.togetherpay.domain.budget.repository;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.trip.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByTripId(Long tripId);
}
