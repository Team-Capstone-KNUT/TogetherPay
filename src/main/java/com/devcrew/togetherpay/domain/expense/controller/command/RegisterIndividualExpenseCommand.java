package com.devcrew.togetherpay.domain.expense.controller.command;

import com.devcrew.togetherpay.domain.expense.Category;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.expense.Expense;
import com.devcrew.togetherpay.domain.expense.PaymentMethod;
import com.devcrew.togetherpay.domain.expense.dto.ParticipantInfo;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.global.common.vo.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record RegisterIndividualExpenseCommand(

    Long tripId,
    String title,
    String description,
    Currency currency,
    Category category,
    LocalDate expenseDate,
    PaymentMethod method,
    List<ParticipantInfo> participantInfos

) {

  public Expense toEntity(Trip trip, BigDecimal totalAmount, BigDecimal exchangeRate) {
    return Expense.builder()
        .title(title)
        .description(description)
        .currency(currency)
        .category(category)
        .expenseDate(expenseDate)
        .paymentMethod(method)
        .totalAmount(Money.of(totalAmount))
        .exchangeRate(Money.of(exchangeRate))
        .trip(trip)
        .build();
  }

}
