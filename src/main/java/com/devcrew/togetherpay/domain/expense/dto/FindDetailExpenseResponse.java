package com.devcrew.togetherpay.domain.expense.dto;

import com.devcrew.togetherpay.domain.expense.Category;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.expense.Expense;
import com.devcrew.togetherpay.domain.expense.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record FindDetailExpenseResponse(

    Long expenseId,
    String title,
    String description,
    BigDecimal totalAmount,
    Integer krwTotalAmount,
    Currency currency,
    Category category,
    LocalDate expenseDate,
    PaymentMethod paymentMethod,
    List<ParticipantResponse> participantResponses,
    boolean isDutchPay

) {

  public static FindDetailExpenseResponse from(Expense expense) {
    return FindDetailExpenseResponse.builder()
            .expenseId(expense.getId())
            .title(expense.getTitle())
            .description(expense.getDescription())
            .totalAmount(expense.getTotalAmount())
            .krwTotalAmount(expense.getKrwTotalAmount())
            .currency(expense.getCurrency())
            .category(expense.getCategory())
            .expenseDate(expense.getExpenseDate())
            .paymentMethod(expense.getPaymentMethod())
            .participantResponses(expense.participantsToResponse())
            .isDutchPay(expense.isDutchPay())
            .build();
  }

}
