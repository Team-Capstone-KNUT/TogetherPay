package com.devcrew.togetherpay.domain.settlement;

import com.devcrew.togetherpay.domain.expense.Expense;
import com.devcrew.togetherpay.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement {
  @Column(name = "settlement_id")
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  private Integer amount;

  @Builder.Default
  @Column(nullable = false)
  Boolean isSettled = false; // 정산 완료 여부

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "expense_id")
  Expense expense;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  User user;

  public static Settlement of(Integer amount, Expense expense, User user) {
    return Settlement.builder()
        .amount(amount)
        .expense(expense)
        .user(user)
        .build();
  }

}
