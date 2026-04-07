package com.devcrew.togetherpay.domain.expense;

import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.global.common.vo.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Participant {

  @Column(name = "participant_id")
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "expense_id")
  private Expense expense;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "amount",
          column = @Column(name = "amount",
              precision = 15, scale = 2,
              nullable = false))
  })
  private Money amount;

  @Column(name = "krw_amount", nullable = true)
  private Integer krwAmount;

  @Column(nullable = false)
  private boolean isPayer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  public static Participant of(Expense expense, User user, boolean isPayer, BigDecimal amount) {
    return Participant.builder()
        .expense(expense)
        .user(user)
        .isPayer(isPayer)
        .amount(Money.of(amount))
        .build();
  }

  public BigDecimal getAmount() {
    return this.amount.getAmount();
  }

  public void calculateKRW(BigDecimal exchangeRate) {
    if (exchangeRate == null) {
      this.krwAmount = amount.toWons();
      return;
    }

    Money result = amount.calculateMultiply(exchangeRate);
    this.krwAmount = result.toWons();
  }

}
