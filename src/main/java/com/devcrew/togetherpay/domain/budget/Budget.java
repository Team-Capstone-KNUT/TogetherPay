package com.devcrew.togetherpay.domain.budget;

import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.global.common.BaseTimeEntity;
import com.devcrew.togetherpay.global.common.vo.Money;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

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
    private Budget(Trip trip, LocalDate budgetDate, Money totalAmount) {
        this.trip = trip;
        this.budgetDate = budgetDate;
        this.totalAmount = totalAmount;
        this.balance = totalAmount; // 처음 생성 지점 잔액은 총 예산과 동일함.
    }

    public static Budget createDailyBudget(Trip trip, LocalDate budgetDate, Money amount) {
        return Budget.builder()
                .trip(trip)
                .budgetDate(budgetDate)
                .totalAmount(amount)
                .build();
    }

    public void spend(Money expenseAmount) {
        // 지출 금액만큼 잔액을 차감합니다. (마이너스 잔액 허용 = 예산 초과 상태)
        this.balance = this.balance.subtract(expenseAmount);
    }

    // 지출 취소 시 잔액 복구 메서드
    public void refund(Money canceledAmount) {
        this.balance = this.balance.add(canceledAmount);
    }

    // 예산 수정 메서드(총 예산)
    public void updateAmount(Money newAmount) {
        // 기존 예산과 새로운 예산 차액 계산(새로운 예산 - 기존 예산)
        Money difference = newAmount.subtract(this.totalAmount);
        // 총 예산 변경
        this.totalAmount = newAmount;
        // 잔액에 차액을 더해 동기화
        this.balance = this.balance.add(difference);
        // 총 예산이 마이너스가 될 경우 예외 발생
        if (this.balance.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BUDGET_CANNOT_BE_LESS_THAN_EXPENSE);
        }
    }
}
