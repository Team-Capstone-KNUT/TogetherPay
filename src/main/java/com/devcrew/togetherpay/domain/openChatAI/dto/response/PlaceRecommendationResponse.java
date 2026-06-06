package com.devcrew.togetherpay.domain.openChatAI.dto.response;

import java.util.List;

public record PlaceRecommendationResponse(
        List<PlaceRecommendationItem> places
) {
}
