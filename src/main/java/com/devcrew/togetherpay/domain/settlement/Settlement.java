package com.devcrew.togetherpay.domain.settlement;

import com.devcrew.togetherpay.domain.expense.Expense;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "expense_id")
  Expense expense;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id")
  private User sender; // 돈을 보내는 유저(정산 인원)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id")
  private User receiver; // 돈을 받는 유저(결제자)

  @Column(nullable = false)
  private boolean isTransferred = false; // 기본값은 false (미송금)

  // 송금 완료 상태 변경 로직
  public void completeTransfer() {
    if (this.isTransferred) {
      throw new BusinessException(ErrorCode.ALREADY_TRANSFERRED);
    }
    this.isTransferred = true;
  }

  public static Settlement of(Long amount, Expense expense, User sender, User receiver) {
    Settlement settlement = Settlement.builder()
        .amount(amount)
        .expense(expense)
        .sender(sender)
        .receiver(receiver)
        .build();

    if (expense != null) {
      expense.getSettlements().add(settlement);
    }

    return settlement;
  }

}
