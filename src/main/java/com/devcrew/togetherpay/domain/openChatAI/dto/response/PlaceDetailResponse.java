package com.devcrew.togetherpay.domain.openChatAI.dto.response;

import java.util.List;

public record PlaceDetailResponse(
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
        String googleMapsUrl,
        MapPreviewResponse mapPreview,
        String websiteUrl,
        String phoneNumber,
        List<String> weekdayDescriptions,
        List<PlacePhotoResponse> photos
) {
}
