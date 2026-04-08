package com.devcrew.togetherpay.domain.budget.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateBudgetRequest(
        @NotNull(message = "변경할 예산 금액 입력은 필수입니다.")
        @Min(value = 0, message = "예산 금액은 0원 이상이어야 합니다.")
        Long amount
) {}
