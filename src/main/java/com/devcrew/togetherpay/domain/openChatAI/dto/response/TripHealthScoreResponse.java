package com.devcrew.togetherpay.domain.openChatAI.dto.response;

import java.util.List;

public record TripHealthScoreResponse(
        Long tripId,
        int score,
        String grade,
        List<String> reasons,
        List<TripHealthImprovementOption> improvementOptions
) {
}
