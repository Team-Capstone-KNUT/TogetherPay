package com.devcrew.togetherpay.domain.expense.dto;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record ParticipantInfo(
    Long userId,
    boolean isPayer, // 결제자 여부
    BigDecimal amount // 개별 금액

) {

    public static ParticipantInfo of(
        Long userId,
        boolean isPayer,
        BigDecimal amount
    ) {
        return ParticipantInfo.builder()
            .userId(userId)
            .isPayer(isPayer)
            .amount(amount)
            .build();
    }
}
