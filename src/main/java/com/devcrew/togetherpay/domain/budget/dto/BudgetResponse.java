package com.devcrew.togetherpay.domain.budget.dto;

import com.devcrew.togetherpay.domain.budget.Budget;

import java.time.LocalDate;

public record BudgetResponse(
        Long BudgetId,
        LocalDate budgetDate,
        Long totalAmount,
        Long balance
) {
    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getBudgetDate(),
                budget.getTotalAmount().getAmount().longValue(),
                budget.getBalance().getAmount().longValue()
        );
    }
}
