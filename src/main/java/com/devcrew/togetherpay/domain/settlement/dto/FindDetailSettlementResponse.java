package com.devcrew.togetherpay.domain.settlement.dto;

import com.devcrew.togetherpay.domain.settlement.Settlement;
import lombok.Builder;

@Builder
public record FindDetailSettlementResponse(
        Long settlementId,
        Long expenseId,
        String expenseTitle,
        String counterpartyNickname,
        String myRole,
        Long amount,
        boolean isTransferred,
        boolean isSettled
) {

  public static FindDetailSettlementResponse of(Settlement settlement, Long myUserId) {

    boolean isSender = settlement.getSender().getId().equals(myUserId);

    String counterpartyName = isSender
            ? settlement.getReceiver().getNickname()
            : settlement.getSender().getNickname();

    String role = isSender ? "SENDER" : "RECEIVER";

    return FindDetailSettlementResponse.builder()
            .settlementId(settlement.getId())
            .expenseId(settlement.getExpense().getId())
            .expenseTitle(settlement.getExpense().getTitle())
            .counterpartyNickname(counterpartyName)
            .myRole(role)
            .amount(settlement.getAmount())
            .isTransferred(settlement.isTransferred())
            .isSettled(settlement.getExpense().isSettled())
            .build();
  }
}