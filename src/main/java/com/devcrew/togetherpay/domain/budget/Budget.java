package com.devcrew.togetherpay.domain.budget;

import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.global.common.BaseTimeEntity;
import com.devcrew.togetherpay.global.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "budgets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private LocalDate budgetDate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount", precision = 15, scale = 2))
    })
    private Money totalAmount; // 설정 총 예산

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "balance", precision = 15, scale = 2))
    })
    private Money balance; // 남은 잔액

    @Builder(access = AccessLevel.PRIVATE)
    private Budget(Team team, LocalDate budgetDate, Money totalAmount) {
        this.team = team;
        this.budgetDate = budgetDate;
        this.totalAmount = totalAmount;
        this.balance = totalAmount; // 처음 생성 지점 잔액은 총 예산과 동일함.
    }

    public static Budget createDailyBudget(Team team, LocalDate budgetDate, Money amount) {
        return Budget.builder()
                .team(team)
                .budgetDate(budgetDate)
                .totalAmount(amount)
                .build();
    }

    public void spend(Money expenseAmount) {
        if (this.balance.getAmount().compareTo(expenseAmount.getAmount())< 0) {

        }

        // 잔액 = 기존 잔액 - 지출 금액
        this.balance = this.balance.subtract(expenseAmount);
    }
}
