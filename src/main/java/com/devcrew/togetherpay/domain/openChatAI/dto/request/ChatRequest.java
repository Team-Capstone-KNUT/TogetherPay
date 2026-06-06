package com.devcrew.togetherpay.domain.openChatAI.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
    @NotBlank(message = "질문을 입력해주세요.")
    String question,
    Long tripId,
    String intent,
    @Valid
    RecommendationCriteria criteria,
    Integer targetScore
) {
    public ChatRequest(String question, Long tripId, String intent, RecommendationCriteria criteria) {
        this(question, tripId, intent, criteria, null);
    }
}
