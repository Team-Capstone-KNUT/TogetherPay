package com.devcrew.togetherpay.domain.expense.dto;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record ParticipantResponse(
    Long participantId,
    String userId,
    String nickname,
    BigDecimal amount,
    boolean isPayer
) {

}
