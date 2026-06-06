package com.devcrew.togetherpay.domain.openChatAI.dto.response;

public record PlaceRecommendationItem(
        String placeId,
        String name,
        String category,
        Double rating,
        Integer reviewCount,
        String address,
        Boolean openNow,
        OpenStatus openStatus,
        String openStatusReason,
        boolean needsVerification,
        Double lat,
        Double lng,
        Integer distanceMeters,
        String googleMapsUrl,
        MapPreviewResponse mapPreview,
        String reason,
        double score
) {
}
