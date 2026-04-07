package com.devcrew.togetherpay.domain.expense.controller.command;

import com.devcrew.togetherpay.domain.expense.Category;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.expense.PaymentMethod;
import com.devcrew.togetherpay.domain.expense.dto.ParticipantInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record UpdateExpenseCommand(
    Boolean isDutchPay,
    Long teamId,
    String title,
    String description,
    Currency currency,
    Category category,
    LocalDate expenseDate,
    PaymentMethod method,
    BigDecimal totalAmount,
    List<ParticipantInfo> participantInfos
) {

}
