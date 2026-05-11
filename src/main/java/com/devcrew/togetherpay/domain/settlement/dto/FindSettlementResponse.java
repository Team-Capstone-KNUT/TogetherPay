package com.devcrew.togetherpay.domain.settlement.dto;

import com.devcrew.togetherpay.domain.settlement.Settlement;
import lombok.Builder;

@Builder
public record FindSettlementResponse(
        Long settlementId,
        Long expenseId,
        String expenseTitle,
        String counterpartyNickname,
        String myRole,
        Long amount,
        boolean isTransferred,
        boolean isSettled
) {
  public static FindSettlementResponse of(Settlement settlement, Long myUserId) {
    boolean isSender = settlement.getSender().getId().equals(myUserId);

    String counterpartyName = isSender
            ? settlement.getReceiver().getNickname()
            : settlement.getSender().getNickname();

    String role = isSender ? "SENDER" : "RECEIVER";

    return FindSettlementResponse.builder()
            .settlementId(settlement.getId())
            .expenseId(settlement.getExpense().getId())
            .expenseTitle(settlement.getExpense().getTitle())
            .counterpartyNickname(counterpartyName)
            .myRole(role)
            .amount(settlement.getAmount())
            .isTransferred(settlement.isTransferred())       // Settlement의 필드
            .isSettled(settlement.getExpense().isSettled()) // Expense의 필드에서 가져옴
            .build();
  }
}