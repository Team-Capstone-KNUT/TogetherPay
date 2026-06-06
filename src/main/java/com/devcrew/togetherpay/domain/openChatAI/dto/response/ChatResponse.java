package com.devcrew.togetherpay.domain.openChatAI.dto.response;

import java.util.List;

public record ChatResponse(
        String message,
        List<ChatBlock> blocks
) {
    public static ChatResponse text(String message) {
        return new ChatResponse(message, List.of());
    }
}
