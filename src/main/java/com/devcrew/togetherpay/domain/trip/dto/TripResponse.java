package com.devcrew.togetherpay.domain.trip.dto;

import com.devcrew.togetherpay.domain.budget.dto.BudgetResponse;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.trip.Trip;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record TripResponse(
        Long tripId,
        Long teamId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Currency baseCurrency,
        List<BudgetResponse> budgets
) {
    public static TripResponse from(Trip trip) {
        return TripResponse.builder()
                .tripId(trip.getId())
                .teamId(trip.getTeam().getId())
                .title(trip.getTitle())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .baseCurrency(trip.getBaseCurrency())
                // 여행에 묶인 예산도 response로 변환해서 내려줌.
                .budgets(trip.getBudgets().stream()
                        .map(BudgetResponse::from)
                        .toList())
                .build();
    }
}