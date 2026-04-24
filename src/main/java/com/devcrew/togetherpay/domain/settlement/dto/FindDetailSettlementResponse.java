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
    Boolean isSettled
) {

  public static FindDetailSettlementResponse of(Settlement settlement, Long myUserId) {

    // 내가 Sender(돈을 보내는 사람)인지 확인
    boolean isSender = settlement.getSender().getId().equals(myUserId);

    // 닉네임 설정 부분, 내가 sender인 경우 상대방을 receiver로, 아니면 반대로 적용
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
            .isSettled(settlement.getIsSettled())
            .build();
  }
}
