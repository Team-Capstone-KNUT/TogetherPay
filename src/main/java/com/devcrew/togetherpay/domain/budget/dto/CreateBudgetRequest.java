package com.devcrew.togetherpay.domain.budget.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateBudgetRequest(
        @NotNull(message = "예산 날짜는 필수입니다.")
        LocalDate budgetDate,

        @NotNull(message = "예산 금액은 필수입니다.")
        @Min(value = 0, message = "예산은 0원 이상이야 합니다.")
        Long amount
) {
}
