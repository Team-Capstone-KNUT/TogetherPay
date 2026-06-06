package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import com.devcrew.togetherpay.domain.openChatAI.google.GooglePhotoMedia;
import com.devcrew.togetherpay.domain.openChatAI.google.GooglePlace;
import com.devcrew.togetherpay.domain.openChatAI.google.GooglePlacesTextSearchRequest;
import com.devcrew.togetherpay.domain.openChatAI.google.GooglePlacesTextSearchResponse;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesClient {

    private static final String TEXT_SEARCH_FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.rating",
            "places.userRatingCount",
            "places.googleMapsUri",
            "places.businessStatus",
            "places.regularOpeningHours",
            "places.primaryType",
            "places.primaryTypeDisplayName"
    );
    private static final String PLACE_DETAIL_FIELD_MASK = String.join(",",
            "id",
            "displayName",
            "formattedAddress",
            "location",
            "rating",
            "userRatingCount",
            "googleMapsUri",
            "websiteUri",
            "nationalPhoneNumber",
            "regularOpeningHours",
            "primaryType",
            "primaryTypeDisplayName",
            "photos"
    );

    private final WebClient.Builder webClientBuilder;

    @Value("${google.places.api-key:}")
    private String apiKey;

    @Value("${google.places.base-url:https://places.googleapis.com}")
    private String placesBaseUrl;

    public List<GooglePlace> searchText(RecommendationCriteria criteria, String includedType, String defaultQuery) {
        validateApiKey();

        GooglePlacesTextSearchRequest request = GooglePlacesTextSearchRequest.of(criteria, includedType, defaultQuery);
        GooglePlacesTextSearchResponse response;

        try {
            response = webClientBuilder
                    .baseUrl(placesBaseUrl)
                    .build()
                    .post()
                    .uri("/v1/places:searchText")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", TEXT_SEARCH_FIELD_MASK)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GooglePlacesTextSearchResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Google Places Text Search failed. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        } catch (WebClientRequestException e) {
            log.warn("Google Places Text Search request failed. message={}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        }

        if (response == null || response.places() == null) {
            return List.of();
        }

        return response.places();
    }

    public GooglePlace getPlaceDetail(String placeId) {
        validateApiKey();

        GooglePlace place;

        try {
            place = webClientBuilder
                    .baseUrl(placesBaseUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("v1", "places", placeId)
                            .build())
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", PLACE_DETAIL_FIELD_MASK)
                    .retrieve()
                    .bodyToMono(GooglePlace.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Google Places Details failed. placeId={}, status={}, body={}", placeId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        } catch (WebClientRequestException e) {
            log.warn("Google Places Details request failed. placeId={}, message={}", placeId, e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        }

        if (place == null || place.id() == null || place.displayName() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        }

        return place;
    }

    public GooglePhotoMedia getPhotoMedia(String placeId, String photoReference, int maxWidthPx, int maxHeightPx) {
        validateApiKey();

        GooglePhotoMedia media;

        try {
            media = webClientBuilder
                    .baseUrl(placesBaseUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("v1", "places", placeId, "photos", photoReference, "media")
                            .queryParam("maxWidthPx", maxWidthPx)
                            .queryParam("maxHeightPx", maxHeightPx)
                            .queryParam("skipHttpRedirect", true)
                            .build())
                    .header("X-Goog-Api-Key", apiKey)
                    .retrieve()
                    .bodyToMono(GooglePhotoMedia.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Google Places Photo Media failed. placeId={}, photoReference={}, status={}, body={}", placeId, photoReference, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        } catch (WebClientRequestException e) {
            log.warn("Google Places Photo Media request failed. placeId={}, photoReference={}, message={}", placeId, photoReference, e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        }

        if (media == null || media.photoUri() == null || media.photoUri().isBlank()) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        }

        return media;
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
