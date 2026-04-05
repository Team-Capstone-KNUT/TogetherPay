package com.devcrew.togetherpay.domain.expense.dto;

import com.devcrew.togetherpay.domain.expense.Category;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.expense.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record FindExpensesResponse(
    List<FindExpenseResponse> findExpenseResponses
) {

  public static FindExpensesResponse from(List<Expense> expenses) {
    List<FindExpenseResponse> responses = expenses.stream()
        .map(expense -> {
          return FindExpenseResponse.builder()
              .expenseId(expense.getId())
              .title(expense.getTitle())
              .currency(expense.getCurrency())
              .totalAmount(expense.getTotalAmount())
              .krwTotalAmount(expense.getKrwTotalAmount())
              .category(expense.getCategory())
              .expenseDate(expense.getExpenseDate())
              .build();
        }).toList();

    return FindExpensesResponse.builder()
        .findExpenseResponses(responses)
        .build();
  }

  @Builder
  public record FindExpenseResponse(
      Long expenseId,
      String title,
      Currency currency,
      BigDecimal totalAmount,
      Integer krwTotalAmount,
      Category category,
      LocalDate expenseDate
  ) {}

}
