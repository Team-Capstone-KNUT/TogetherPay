package com.devcrew.togetherpay.domain.openChatAI.dto.response;

import com.devcrew.togetherpay.domain.expense.Category;

public record BudgetCategoryInsight(
        Category category,
        Long spentAmount
) {
}
