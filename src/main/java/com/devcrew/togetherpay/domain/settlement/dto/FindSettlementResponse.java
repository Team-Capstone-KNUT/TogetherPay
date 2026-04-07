package com.devcrew.togetherpay.domain.settlement.dto;

import com.devcrew.togetherpay.domain.settlement.Settlement;

public record FindSettlementResponse(
    Long settlementId,
    String expenseTitle,
    String nickname,
    Integer amount,
    Boolean isSettled
) {
  public static FindSettlementResponse of(Settlement settlement) {
    return new FindSettlementResponse(
        settlement.getId(),
        settlement.getExpense().getTitle(),
        settlement.getUser().getNickname(),
        settlement.getAmount(),
        settlement.getIsSettled()
    );
  }
}