package com.devcrew.togetherpay.domain.expense;

import com.devcrew.togetherpay.domain.expense.dto.ParticipantResponse;
import jakarta.persistence.Column;
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

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

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
        .amount(amount)
        .build();
  }

}
