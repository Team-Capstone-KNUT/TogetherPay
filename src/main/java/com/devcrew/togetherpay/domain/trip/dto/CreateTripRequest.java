package com.devcrew.togetherpay.domain.trip.dto;

import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.trip.dto.command.CreateTripCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record CreateTripRequest(
        @NotNull(message = "팀 ID는 필수입니다.")
        Long teamId,

        @NotBlank(message = "여행 제목을 입력해주세요.")
        String title,

        @NotNull(message = "여행 시작일을 선택해주세요.")
        LocalDate startDate,

        @NotNull(message = "여행 종료일을 선택해주세요.")
        LocalDate endDate,

        @NotNull(message = "기준 통화를 선택해주세요.")
        Currency baseCurrency,

        @NotNull(message = "총 예산을 입력해주세요.")
        BigDecimal totalBudget
) {
    public CreateTripCommand toCommand() {
        return CreateTripCommand.builder()
                .teamId(teamId)
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .baseCurrency(baseCurrency)
                .totalBudget(totalBudget)
                .build();
    }
}