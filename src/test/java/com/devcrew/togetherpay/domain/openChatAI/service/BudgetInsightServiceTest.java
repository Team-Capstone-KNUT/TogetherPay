package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.budget.repository.BudgetRepository;
import com.devcrew.togetherpay.domain.expense.Category;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.expense.dto.ExpenseCategoryTotal;
import com.devcrew.togetherpay.domain.expense.repository.ExpenseRepository;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.BudgetInsightResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatResponse;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.global.common.vo.Money;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BudgetInsightServiceTest {

    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
    private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final BudgetInsightService budgetInsightService = new BudgetInsightService(
            budgetRepository,
            expenseRepository,
            chatClient
    );

    @Test
    void returnsBudgetInsightBlockWithCalculatedUsageRate() {
        Budget budget = Budget.createBudget(trip(), Money.wons(100_000));
        budget.spend(Money.wons(65_000));
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.of(budget));
        when(expenseRepository.sumTotalAmountByTripId(11L)).thenReturn(BigDecimal.valueOf(65_000));
        when(expenseRepository.sumTotalAmountByTripIdGroupByCategory(11L)).thenReturn(List.of(
                new ExpenseCategoryTotal(Category.MEAL, BigDecimal.valueOf(40_000)),
                new ExpenseCategoryTotal(Category.TRANSPORT, BigDecimal.valueOf(15_000))
        ));
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("현재 예산 흐름은 안정적입니다.");

        ChatResponse response = budgetInsightService.analyze(11L, "이번 여행 예산 괜찮아?");

        assertThat(response.message()).isEqualTo("현재 예산 흐름은 안정적입니다.");
        assertThat(response.blocks()).hasSize(1);
        assertThat(response.blocks().get(0).type()).isEqualTo("budget_insight");
        assertThat(response.blocks().get(0).data()).isInstanceOf(BudgetInsightResponse.class);

        BudgetInsightResponse insight = (BudgetInsightResponse) response.blocks().get(0).data();
        assertThat(insight.currency()).isEqualTo("KRW");
        assertThat(insight.totalBudget()).isEqualTo(100_000L);
        assertThat(insight.spentAmount()).isEqualTo(65_000L);
        assertThat(insight.remainingAmount()).isEqualTo(35_000L);
        assertThat(insight.usageRate()).isEqualTo(65.0);
        assertThat(insight.status()).isEqualTo("OK");
        assertThat(insight.topCategories()).hasSize(2);
    }

    @Test
    void returnsNoBudgetStatusWhenBudgetDoesNotExist() {
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.empty());
        when(expenseRepository.sumTotalAmountByTripId(11L)).thenReturn(BigDecimal.valueOf(25_000));
        when(expenseRepository.sumTotalAmountByTripIdGroupByCategory(11L)).thenReturn(List.of());
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("예산 설정이 필요합니다.");

        ChatResponse response = budgetInsightService.analyze(11L, "예산 체크해줘");

        BudgetInsightResponse insight = (BudgetInsightResponse) response.blocks().get(0).data();
        assertThat(insight.currency()).isNull();
        assertThat(insight.totalBudget()).isNull();
        assertThat(insight.remainingAmount()).isNull();
        assertThat(insight.spentAmount()).isEqualTo(25_000L);
        assertThat(insight.status()).isEqualTo("NO_BUDGET");
    }

    @Test
    void fallsBackWhenOpenAiFails() {
        Budget budget = Budget.createBudget(trip(), Money.wons(100_000));
        budget.spend(Money.wons(85_000));
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.of(budget));
        when(expenseRepository.sumTotalAmountByTripId(11L)).thenReturn(BigDecimal.valueOf(85_000));
        when(expenseRepository.sumTotalAmountByTripIdGroupByCategory(11L)).thenReturn(List.of());
        when(chatClient.prompt().user(anyString()).call().content()).thenThrow(new RuntimeException("openai failed"));

        ChatResponse response = budgetInsightService.analyze(11L, "예산 체크해줘");

        assertThat(response.message()).contains("현재까지 총 100,000 KRW 중 85,000 KRW를 사용");
        BudgetInsightResponse insight = (BudgetInsightResponse) response.blocks().get(0).data();
        assertThat(insight.status()).isEqualTo("WARNING");
    }

    @Test
    void calculatesSpentAmountFromBudgetBalanceInsteadOfKrwExpenseAmount() {
        Budget budget = Budget.createBudget(jpyTrip(), Money.of(BigDecimal.valueOf(100_000)));
        budget.spend(Money.of(BigDecimal.valueOf(4_000)));
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.of(budget));
        when(expenseRepository.sumTotalAmountByTripId(11L)).thenReturn(BigDecimal.valueOf(4_000));
        when(expenseRepository.sumTotalAmountByTripIdGroupByCategory(11L)).thenReturn(List.of(
                new ExpenseCategoryTotal(Category.MEAL, BigDecimal.valueOf(4_000))
        ));
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("JPY 예산 기준으로 안정적입니다.");

        ChatResponse response = budgetInsightService.analyze(11L, "예산 체크해줘");

        BudgetInsightResponse insight = (BudgetInsightResponse) response.blocks().get(0).data();
        assertThat(insight.currency()).isEqualTo("JPY");
        assertThat(insight.totalBudget()).isEqualTo(100_000L);
        assertThat(insight.spentAmount()).isEqualTo(4_000L);
        assertThat(insight.remainingAmount()).isEqualTo(96_000L);
        assertThat(insight.usageRate()).isEqualTo(4.0);
        assertThat(insight.topCategories().get(0).spentAmount()).isEqualTo(4_000L);
    }

    private Trip trip() {
        return Trip.builder()
                .id(11L)
                .title("도쿄")
                .startDate(LocalDate.of(2026, 6, 10))
                .endDate(LocalDate.of(2026, 6, 12))
                .baseCurrency(Currency.KRW)
                .build();
    }

    private Trip jpyTrip() {
        return Trip.builder()
                .id(11L)
                .title("도쿄")
                .startDate(LocalDate.of(2026, 6, 10))
                .endDate(LocalDate.of(2026, 6, 12))
                .baseCurrency(Currency.JPY)
                .build();
    }
}
