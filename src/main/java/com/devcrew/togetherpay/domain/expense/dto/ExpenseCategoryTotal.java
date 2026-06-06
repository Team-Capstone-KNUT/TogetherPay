package com.devcrew.togetherpay.domain.expense.dto;

import com.devcrew.togetherpay.domain.expense.Category;

import java.math.BigDecimal;

public record ExpenseCategoryTotal(
        Category category,
        BigDecimal amount
) {
}
