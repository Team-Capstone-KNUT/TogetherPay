package com.devcrew.togetherpay.domain.trip.dto;

import com.devcrew.togetherpay.domain.budget.dto.BudgetResponse;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.trip.Trip;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record TripResponse(
        Long tripId,
        Long teamId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Currency baseCurrency,
        BudgetResponse budget
) {
    public static TripResponse from(Trip trip) {
        return TripResponse.builder()
                .tripId(trip.getId())
                .teamId(trip.getTeam().getId())
                .title(trip.getTitle())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .baseCurrency(trip.getBaseCurrency())
                .budget(trip.getBudget() != null ? BudgetResponse.from(trip.getBudget()) : null)
                .build();
    }
}