package com.devcrew.togetherpay.domain.expense;

import com.devcrew.togetherpay.domain.expense.dto.FindExpensesResponse;
import com.devcrew.togetherpay.global.common.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpensePaymentMethodTest {

    @Test
    void includesPaymentMethodInExpenseListResponse() {
        Expense expense = expense(PaymentMethod.CARD);

        FindExpensesResponse response = FindExpensesResponse.from(List.of(expense));

        assertThat(response.findExpenseResponses()).hasSize(1);
        assertThat(response.findExpenseResponses().get(0).paymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void updatesPaymentMethodFromCardToCash() {
        Expense expense = expense(PaymentMethod.CARD);

        expense.updateInfo(
                "현금 결제로 변경",
                "수정",
                Currency.KRW,
                Category.MEAL,
                LocalDate.of(2026, 6, 10),
                PaymentMethod.CASH,
                BigDecimal.valueOf(10_000),
                null
        );

        assertThat(expense.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    private Expense expense(PaymentMethod paymentMethod) {
        return Expense.builder()
                .id(1L)
                .title("식비")
                .description("라멘")
                .currency(Currency.KRW)
                .category(Category.MEAL)
                .expenseDate(LocalDate.of(2026, 6, 10))
                .paymentMethod(paymentMethod)
                .totalAmount(Money.wons(10_000))
                .krwTotalAmount(10_000)
                .participants(List.of())
                .build();
    }
}
