package com.devcrew.togetherpay.domain.expense.repository;

import com.devcrew.togetherpay.domain.expense.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

}
