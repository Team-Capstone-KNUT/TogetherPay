package com.devcrew.togetherpay.domain.openChatAI.dto.response;

import java.util.List;

public record TripHealthImprovementResponse(
        Long tripId,
        int currentScore,
        int targetScore,
        int projectedScore,
        int expectedGain,
        String currentGrade,
        String targetGrade,
        List<String> focusAreas,
        List<TripHealthImprovementStep> steps
) {
}
