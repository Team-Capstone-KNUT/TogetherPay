package com.devcrew.togetherpay.domain.expense.service;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.budget.repository.BudgetRepository;
import com.devcrew.togetherpay.domain.expense.Category;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.expense.PaymentMethod;
import com.devcrew.togetherpay.domain.expense.controller.command.RegisterDutchExpenseCommand;
import com.devcrew.togetherpay.domain.expense.dto.ParticipantInfo;
import com.devcrew.togetherpay.domain.expense.repository.ExpenseRepository;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.TeamUser;
import com.devcrew.togetherpay.domain.team.repository.TeamUserRepository;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.domain.trip.repository.TripRepository;
import com.devcrew.togetherpay.domain.user.Provider;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.UserRole;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.common.ExchangeRate.service.ExchangeRateService;
import com.devcrew.togetherpay.global.common.vo.Money;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseServiceTest {

    private final TripRepository tripRepository = mock(TripRepository.class);
    private final TeamUserRepository teamUserRepository = mock(TeamUserRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
    private final ExchangeRateService exchangeRateService = mock(ExchangeRateService.class);
    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final ExpenseService expenseService = new ExpenseService(
            tripRepository,
            teamUserRepository,
            userRepository,
            expenseRepository,
            exchangeRateService,
            budgetRepository
    );

    @Test
    void spendsBudgetInTripBaseCurrencyWhenExpenseCurrencyDiffers() {
        User payer = user(1L, "payer");
        User member = user(2L, "member");
        Team team = Team.createTeam("도쿄팀", "1234");
        TeamUser.createLeader(team, payer);
        TeamUser.createMember(team, member);
        Trip trip = trip(team, Currency.JPY);
        Budget budget = Budget.createBudget(trip, Money.of(BigDecimal.valueOf(100_000)));
        RegisterDutchExpenseCommand command = RegisterDutchExpenseCommand.builder()
                .tripId(11L)
                .title("식비")
                .description("라멘")
                .currency(Currency.KRW)
                .category(Category.MEAL)
                .expenseDate(LocalDate.of(2026, 6, 10))
                .method(PaymentMethod.CARD)
                .totalAmount(BigDecimal.valueOf(40_000))
                .participantInfos(List.of(
                        ParticipantInfo.of(1L, true, BigDecimal.ZERO),
                        ParticipantInfo.of(2L, false, BigDecimal.ZERO)
                ))
                .build();

        when(tripRepository.findById(11L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
        when(teamUserRepository.existsByTeamAndUser(team, payer)).thenReturn(true);
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(payer, member));
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.of(budget));
        when(exchangeRateService.getExchangeRate(Currency.KRW, LocalDate.of(2026, 6, 10))).thenReturn(null);
        when(exchangeRateService.getExchangeRate(Currency.JPY, LocalDate.of(2026, 6, 10))).thenReturn(BigDecimal.TEN);
        when(expenseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.registerWithDutchPay(1L, command);

        assertThat(budget.getRemainingAmount().getAmount()).isEqualByComparingTo("96000");
    }

    @Test
    void storesSelectedPaymentMethodWhenRegisteringExpense() {
        User payer = user(1L, "payer");
        Team team = Team.createTeam("도쿄팀", "1234");
        TeamUser.createLeader(team, payer);
        Trip trip = trip(team, Currency.KRW);
        Budget budget = Budget.createBudget(trip, Money.wons(100_000));
        RegisterDutchExpenseCommand command = RegisterDutchExpenseCommand.builder()
                .tripId(11L)
                .title("현금 식비")
                .description("시장")
                .currency(Currency.KRW)
                .category(Category.MEAL)
                .expenseDate(LocalDate.of(2026, 6, 10))
                .method(PaymentMethod.CASH)
                .totalAmount(BigDecimal.valueOf(10_000))
                .participantInfos(List.of(ParticipantInfo.of(1L, true, BigDecimal.ZERO)))
                .build();

        when(tripRepository.findById(11L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
        when(teamUserRepository.existsByTeamAndUser(team, payer)).thenReturn(true);
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(payer));
        when(budgetRepository.findByTripId(11L)).thenReturn(Optional.of(budget));
        when(exchangeRateService.getExchangeRate(Currency.KRW, LocalDate.of(2026, 6, 10))).thenReturn(null);

        expenseService.registerWithDutchPay(1L, command);

        ArgumentCaptor<com.devcrew.togetherpay.domain.expense.Expense> captor =
                ArgumentCaptor.forClass(com.devcrew.togetherpay.domain.expense.Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    private User user(Long id, String nickname) {
        User user = User.builder()
                .email(nickname + "@example.com")
                .nickname(nickname)
                .role(UserRole.USER)
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(id))
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Trip trip(Team team, Currency baseCurrency) {
        return Trip.builder()
                .id(11L)
                .title("도쿄")
                .startDate(LocalDate.of(2026, 6, 10))
                .endDate(LocalDate.of(2026, 6, 12))
                .baseCurrency(baseCurrency)
                .team(team)
                .build();
    }
}
