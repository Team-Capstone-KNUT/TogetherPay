package com.devcrew.togetherpay.domain.openChatAI;

import java.util.List;

public record TripSelectionContext(
        Long teamId,
        List<Long> tripIds
) {
}
