package com.devcrew.togetherpay.domain.settlement.dto;

import com.devcrew.togetherpay.domain.settlement.Settlement;
import lombok.Builder;

@Builder
public record FindSettlementResponse(
    Long settlementId,
    String expenseTitle,
    String counterpartyNickname, // 상대방 닉네임
    String myRole, // SENDER(본인이 낼 돈)인지 RECEIVER(본인이 받을 돈)인지 확인
    Long amount,
    Boolean isSettled
) {
  public static FindSettlementResponse of(Settlement settlement, Long myUserId) {

    // 내가 돈을 보내는 사람인 경우
    boolean isSender = settlement.getSender().getId().equals(myUserId);

    // 상대방 닉네임을 결정(내가 Sender인 경우 상대방은 Receiver, 반대인 경우에는 반대로)
    String counterpartyName = isSender
        ? settlement.getReceiver().getNickname()
        : settlement.getSender().getNickname();

    String role = isSender ? "SENDER" : "RECEIVER";

    return FindSettlementResponse.builder()
        .settlementId(settlement.getId())
        .expenseTitle(settlement.getExpense().getTitle())
        .counterpartyNickname(counterpartyName) // "누구에게/누구로부터"
        .myRole(role)                           // 상태 라벨용
        .amount(settlement.getAmount())
        .isSettled(settlement.getIsSettled())
        .build();
  }
}