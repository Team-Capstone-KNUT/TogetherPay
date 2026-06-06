package com.devcrew.togetherpay.domain.openChatAI.dto.response;

public record MapPreviewResponse(
        String provider,
        String placeId,
        Double lat,
        Double lng,
        String googleMapsUrl,
        boolean previewAvailable
) {
}
