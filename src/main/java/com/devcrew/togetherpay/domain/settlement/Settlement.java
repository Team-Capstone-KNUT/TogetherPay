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
  private Long amount;

  @Builder.Default
  @Column(nullable = false)
  Boolean isSettled = false; // 정산 완료 여부

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "expense_id")
  Expense expense;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id")
  private User sender; // 돈을 보내는 유저(정산 인원)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id")
  private User receiver; // 돈을 받는 유저(결제자)

  public static Settlement of(Long amount, Expense expense, User sender, User receiver) {
    return Settlement.builder()
        .amount(amount)
        .expense(expense)
        .sender(sender)
        .receiver(receiver)
        .build();
  }

}
