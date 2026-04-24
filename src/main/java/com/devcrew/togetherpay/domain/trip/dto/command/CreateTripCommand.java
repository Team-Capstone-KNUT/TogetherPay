package com.devcrew.togetherpay.domain.trip.dto.command;

import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.trip.Trip;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record CreateTripCommand(
        Long teamId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Currency baseCurrency,
        BigDecimal totalBudget
) {
    public Trip toEntity(Team team) {
        return Trip.builder()
                .team(team)
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .baseCurrency(baseCurrency)
                .build();
    }
}