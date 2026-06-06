package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.budget.repository.BudgetRepository;
import com.devcrew.togetherpay.domain.expense.Category;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.expense.dto.ExpenseCategoryTotal;
import com.devcrew.togetherpay.domain.expense.repository.ExpenseRepository;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.TripHealthImprovementResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.TripHealthScoreResponse;
import com.devcrew.togetherpay.domain.schedule.Schedule;
import com.devcrew.togetherpay.domain.schedule.ScheduleItem;
import com.devcrew.togetherpay.domain.schedule.repository.ScheduleRepository;
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

class TripHealthScoreServiceTest {

    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
    private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final TripHealthScoreService service = new TripHealthScoreService(
            scheduleRepository,
            budgetRepository,
            expenseRepository,
            chatClient
    );

    @Test
    void returnsTripHealthScoreBlockWithRuleBasedReasons() {
        when(scheduleRepository.findByTripId(11L)).thenReturn(Optional.of(scheduleWithDenseDay()));
        Budget budget = Budget.createBudget(trip(), Money.wons(100_000));
        budget.spend(Money.wons(85_000));
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.of(budget));
        when(expenseRepository.sumTotalAmountByTripIdGroupByCategory(11L)).thenReturn(List.of(
                new ExpenseCategoryTotal(Category.MEAL, BigDecimal.valueOf(70_000))
        ));
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("이번 여행의 건강도는 보완이 필요합니다.");

        ChatResponse response = service.analyze(11L, "여행 건강도 알려줘");

        assertThat(response.message()).isEqualTo("이번 여행의 건강도는 보완이 필요합니다.");
        assertThat(response.blocks()).hasSize(1);
        assertThat(response.blocks().get(0).type()).isEqualTo("trip_health_score");
        assertThat(response.blocks().get(0).data()).isInstanceOf(TripHealthScoreResponse.class);

        TripHealthScoreResponse score = (TripHealthScoreResponse) response.blocks().get(0).data();
        assertThat(score.tripId()).isEqualTo(11L);
        assertThat(score.score()).isLessThan(100);
        assertThat(score.grade()).isIn("GOOD", "WARNING", "RISKY");
        assertThat(score.reasons())
                .contains("하루 일정이 다소 빽빽한 날짜가 있습니다.")
                .contains("예산 사용률이 80% 이상이라 남은 일정의 지출 관리가 필요합니다.")
                .contains("지출 카테고리가 적어 여행 경험이 한쪽으로 치우쳐 보일 수 있습니다.");
        assertThat(score.improvementOptions())
                .extracting("targetScore")
                .containsExactly(80, 90, 100);
    }

    @Test
    void fallsBackWhenOpenAiFails() {
        when(scheduleRepository.findByTripId(11L)).thenReturn(Optional.empty());
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.empty());
        when(expenseRepository.sumTotalAmountByTripIdGroupByCategory(11L)).thenReturn(List.of());
        when(chatClient.prompt().user(anyString()).call().content()).thenThrow(new RuntimeException("openai failed"));

        ChatResponse response = service.analyze(11L, "여행 건강도 알려줘");

        assertThat(response.message()).contains("이번 여행의 건강도는");
        TripHealthScoreResponse score = (TripHealthScoreResponse) response.blocks().get(0).data();
        assertThat(score.score()).isEqualTo(45);
        assertThat(score.grade()).isEqualTo("RISKY");
    }

    @Test
    void returnsTripHealthImprovementBlock() {
        when(scheduleRepository.findByTripId(11L)).thenReturn(Optional.of(scheduleWithDenseDay()));
        Budget budget = Budget.createBudget(trip(), Money.wons(100_000));
        budget.spend(Money.wons(85_000));
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.of(budget));
        when(expenseRepository.sumTotalAmountByTripIdGroupByCategory(11L)).thenReturn(List.of(
                new ExpenseCategoryTotal(Category.MEAL, BigDecimal.valueOf(70_000))
        ));
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("90점에 가까워지려면 일정과 예산을 보완하세요.");

        ChatResponse response = service.improve(11L, "90점으로 개선해줘", 90);

        assertThat(response.message()).isEqualTo("90점에 가까워지려면 일정과 예산을 보완하세요.");
        assertThat(response.blocks()).hasSize(1);
        assertThat(response.blocks().get(0).type()).isEqualTo("trip_health_improvement");
        assertThat(response.blocks().get(0).data()).isInstanceOf(TripHealthImprovementResponse.class);

        TripHealthImprovementResponse improvement = (TripHealthImprovementResponse) response.blocks().get(0).data();
        assertThat(improvement.tripId()).isEqualTo(11L);
        assertThat(improvement.targetScore()).isEqualTo(90);
        assertThat(improvement.currentScore()).isLessThan(90);
        assertThat(improvement.steps()).isNotEmpty();
        assertThat(improvement.expectedGain()).isEqualTo(improvement.steps().stream()
                .mapToInt(step -> step.expectedGain())
                .sum());
        assertThat(improvement.projectedScore()).isBetween(improvement.currentScore(), improvement.targetScore());
    }

    @Test
    void normalizesTripHealthImprovementTargetScore() {
        when(scheduleRepository.findByTripId(11L)).thenReturn(Optional.empty());
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.empty());
        when(expenseRepository.sumTotalAmountByTripIdGroupByCategory(11L)).thenReturn(List.of());
        when(chatClient.prompt().user(anyString()).call().content()).thenThrow(new RuntimeException("openai failed"));

        ChatResponse response = service.improve(11L, "95점으로 개선해줘", 95);

        TripHealthImprovementResponse improvement = (TripHealthImprovementResponse) response.blocks().get(0).data();
        assertThat(improvement.targetScore()).isEqualTo(90);
        assertThat(response.message()).contains("90점에 가까워지려면");
    }

    @Test
    void selectsMoreImprovementStepsForHigherTargetScore() {
        when(scheduleRepository.findByTripId(11L)).thenReturn(Optional.of(scheduleWithDenseDay()));
        Budget budget = Budget.createBudget(trip(), Money.wons(100_000));
        budget.spend(Money.wons(85_000));
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.of(budget));
        when(expenseRepository.sumTotalAmountByTripIdGroupByCategory(11L)).thenReturn(List.of(
                new ExpenseCategoryTotal(Category.MEAL, BigDecimal.valueOf(70_000))
        ));
        when(chatClient.prompt().user(anyString()).call().content()).thenThrow(new RuntimeException("openai failed"));

        ChatResponse improveTo80 = service.improve(11L, "80점으로 개선해줘", 80);
        ChatResponse improveTo100 = service.improve(11L, "100점으로 개선해줘", 100);

        TripHealthImprovementResponse to80 = (TripHealthImprovementResponse) improveTo80.blocks().get(0).data();
        TripHealthImprovementResponse to100 = (TripHealthImprovementResponse) improveTo100.blocks().get(0).data();

        assertThat(to80.steps()).isNotEmpty();
        assertThat(to100.steps().size()).isGreaterThan(to80.steps().size());
        assertThat(to100.expectedGain()).isGreaterThan(to80.expectedGain());
        assertThat(to100.projectedScore()).isGreaterThanOrEqualTo(to80.projectedScore());
    }

    private Schedule scheduleWithDenseDay() {
        LocalDate day = LocalDate.of(2026, 6, 10);
        return Schedule.builder()
                .id(1L)
                .startDate(day)
                .endDate(day.plusDays(1))
                .trip(trip())
                .scheduleItems(List.of(
                        item(day, "신주쿠", "쇼핑"),
                        item(day, "라멘", "점심"),
                        item(day, "카페", "휴식"),
                        item(day, "전망대", "야경"),
                        item(day, "돈키호테", "쇼핑"),
                        item(day, "이자카야", "저녁"),
                        item(day.plusDays(1), null, null)
                ))
                .build();
    }

    private ScheduleItem item(LocalDate date, String title, String description) {
        return ScheduleItem.builder()
                .date(date)
                .title(title)
                .description(description)
                .build();
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
}
