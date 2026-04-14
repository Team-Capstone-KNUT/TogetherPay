package com.devcrew.togetherpay.domain.settlement.dto;

import com.devcrew.togetherpay.domain.settlement.Settlement;
import lombok.Builder;

@Builder
public record FindDetailSettlementResponse(
    Long settlementId,
    Long expenseId,
    Long amount,
    Boolean isSettled,
    String expenseTitle,
    String nickname
) {

  public static FindDetailSettlementResponse of(Settlement settlement, String nickname) {
    return FindDetailSettlementResponse.builder()
        .settlementId(settlement.getId())
        .expenseId(settlement.getExpense().getId())
        .amount(settlement.getAmount())
        .isSettled(settlement.getIsSettled())
        .expenseTitle(settlement.getExpense().getTitle())
        .nickname(nickname)
        .build();
  }
}
