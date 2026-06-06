package com.devcrew.togetherpay.domain.openChatAI.dto.response;

public record PlacePhotoResponse(
        String name,
        String photoReference,
        Integer widthPx,
        Integer heightPx
) {
}
