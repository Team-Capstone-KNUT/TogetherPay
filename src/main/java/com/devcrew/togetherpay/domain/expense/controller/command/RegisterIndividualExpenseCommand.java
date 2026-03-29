package com.devcrew.togetherpay.domain.expense.controller.command;

import com.devcrew.togetherpay.domain.expense.Category;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.expense.Expense;
import com.devcrew.togetherpay.domain.expense.PaymentMethod;
import com.devcrew.togetherpay.domain.expense.dto.ParticipantInfo;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record RegisterIndividualExpenseCommand(

    Long teamId,
    String title,
    String description,
    Currency currency,
    Category category,
    PaymentMethod method,
    List<ParticipantInfo> participantInfos

) {

  public Expense toEntity(Team team, BigDecimal totalAmount) {
    return Expense.builder()
        .title(title)
        .description(description)
        .category(category)
        .paymentMethod(method)
        .totalAmount(totalAmount)
        .team(team)
        .build();
  }

}
