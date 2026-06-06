package com.devcrew.togetherpay.domain.openChatAI.dto.response;

import java.util.List;

public record BudgetInsightResponse(
        Long tripId,
        String currency,
        Long totalBudget,
        Long spentAmount,
        Long remainingAmount,
        double usageRate,
        String status,
        List<BudgetCategoryInsight> topCategories
) {
}
