package com.devcrew.togetherpay.domain.openChatAI.google;

import com.devcrew.togetherpay.domain.openChatAI.service.PlaceOpenStatusResolver.OpeningHours;

import java.util.List;

public record GooglePlace(
        String id,
        GoogleLocalizedText displayName,
        String formattedAddress,
        GoogleLatLng location,
        Double rating,
        Integer userRatingCount,
        String googleMapsUri,
        String websiteUri,
        String nationalPhoneNumber,
        String businessStatus,
        OpeningHours regularOpeningHours,
        String primaryType,
        GoogleLocalizedText primaryTypeDisplayName,
        List<GooglePhoto> photos
) {
}
