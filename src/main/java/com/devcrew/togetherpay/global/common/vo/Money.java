package com.devcrew.togetherpay.global.common.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    public static Money wons(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    // 원화로 변환.
    public Integer toWons(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).intValue(); // 소수점 x 반올림.
    }
    
    public Integer toWons() {
        return amount.setScale(0, RoundingMode.HALF_UP).intValue(); // 소수점 x 반올림.
    }
    
    public Money calculateMultiply(BigDecimal exchangeRate) {
        BigDecimal result = this.getAmount()
            .multiply(exchangeRate)
            .setScale(2, RoundingMode.HALF_UP); // 소수점 2자리 반올림
    
        return Money.of(result);
    }

    private Money(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0보다 작을 수 없습니다.");
        }
        this.amount = amount;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money plus(Money other) {
        return new Money(this.amount.add(other.getAmount()));
    }

    public Money minus(Money other) {
        return new Money(this.amount.subtract(other.getAmount()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }
}
