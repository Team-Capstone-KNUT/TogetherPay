package com.devcrew.togetherpay.domain.openChatAI.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
    @NotBlank(message = "질문을 입력해주세요.")
    String question
) {}
