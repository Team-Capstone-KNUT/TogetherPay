package com.devcrew.togetherpay.domain.openChatAI.google;

import java.util.List;

public record GooglePlacesTextSearchResponse(
        List<GooglePlace> places
) {
}
