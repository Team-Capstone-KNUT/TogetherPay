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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount"))
    })
    private Money totalAmount; // 설정 총 예산

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "remaining_amount"))
    })
    private Money remainingAmount; // 남은 잔액

    private Budget(Trip trip, Money totalAmount) {
        this.trip = trip;
        this.totalAmount = totalAmount;
        this.remainingAmount = totalAmount; // 처음 생성 지점 잔액은 총 예산과 동일함.
    }

    public static Budget createBudget(Trip trip, Money totalAmount) {
        return new Budget(trip, totalAmount);
    }

    // 예산 금액 수정 기능
    public void updateAmount(Money newTotalAmount) {
        // 현재까지 사용한 금액 = (기존 총액) - (기존 잔액)
        Money spentAmount = this.totalAmount.minus(this.remainingAmount);

        // 총액을 새로운 금액으로 교체
        this.totalAmount = newTotalAmount;

        // 새로운 잔액 = (새로운 총액) - (사용한 금액)
        this.remainingAmount = newTotalAmount.minus(spentAmount);
    }

    public void spend(Money amount) {
        // 지출 금액만큼 잔액을 차감합니다. (마이너스 잔액 허용 = 예산 초과 상태)
        this.remainingAmount = this.remainingAmount.minus(amount);
    }

    // 지출 취소 시 잔액 복구 메서드
    public void refund(Money amount) {
        this.remainingAmount = this.remainingAmount.plus(amount);
    }


}
