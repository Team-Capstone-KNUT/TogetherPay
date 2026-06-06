package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.TransportMode;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.MapPreviewResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlaceDetailResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlacePhotoMediaResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlacePhotoResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlaceRecommendationItem;
import com.devcrew.togetherpay.domain.openChatAI.google.GooglePhotoMedia;
import com.devcrew.togetherpay.domain.openChatAI.google.GooglePlace;
import com.devcrew.togetherpay.domain.openChatAI.service.PlaceOpenStatusResolver.OpenStatusResult;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GooglePlacesService {

    private static final String SEARCH_CACHE_KEY_PREFIX = "places:search:";
    private static final String DETAIL_CACHE_KEY_PREFIX = "places:detail:";
    private static final String PHOTO_MEDIA_CACHE_KEY_PREFIX = "places:photo-media:";
    private final PlaceOpenStatusResolver placeOpenStatusResolver;
    private final PlaceDistanceCalculator placeDistanceCalculator;
    private final PlaceRecommendationScorer placeRecommendationScorer;
    private final GooglePlacesCacheService googlePlacesCacheService;
    private final GooglePlacesClient googlePlacesClient;

    public List<PlaceRecommendationItem> searchRestaurants(RecommendationCriteria criteria) {
        return searchPlaces(criteria, "restaurant", "맛집");
    }

    public List<PlaceRecommendationItem> searchCafes(RecommendationCriteria criteria) {
        return searchPlaces(criteria, "cafe", "카페");
    }

    public List<PlaceRecommendationItem> searchAttractions(RecommendationCriteria criteria) {
        return searchPlaces(criteria, "tourist_attraction", "관광지");
    }

    private List<PlaceRecommendationItem> searchPlaces(
            RecommendationCriteria criteria,
            String includedType,
            String defaultQuery
    ) {
        String cacheKey = buildSearchCacheKey(criteria, includedType, defaultQuery);
        List<PlaceRecommendationItem> cachedItems = googlePlacesCacheService.getSearchResult(cacheKey);
        if (cachedItems != null) {
            return cachedItems;
        }

        List<PlaceRecommendationItem> items = googlePlacesClient.searchText(criteria, includedType, defaultQuery).stream()
                .filter(place -> place.id() != null && place.displayName() != null)
                .map(place -> toRecommendationItem(criteria, place, includedType))
                .filter(item -> isWithinRequestedWalkingDistance(criteria, item))
                .sorted(Comparator.comparingDouble(PlaceRecommendationItem::score).reversed())
                .toList();

        googlePlacesCacheService.cacheSearchResult(cacheKey, items);

        return items;
    }

    public PlaceDetailResponse getPlaceDetail(String placeId, OffsetDateTime visitDateTime) {
        if (placeId == null || placeId.isBlank() || placeId.contains("/")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String cacheKey = buildDetailCacheKey(placeId, visitDateTime);
        PlaceDetailResponse cachedDetail = googlePlacesCacheService.getPlaceDetail(cacheKey);
        if (cachedDetail != null) {
            return cachedDetail;
        }

        GooglePlace place = googlePlacesClient.getPlaceDetail(placeId);
        PlaceDetailResponse detail = toPlaceDetailResponse(place, visitDateTime);
        googlePlacesCacheService.cachePlaceDetail(cacheKey, detail);

        return detail;
    }

    public PlacePhotoMediaResponse getPlacePhotoMedia(
            String placeId,
            String photoReference,
            Integer maxWidthPx,
            Integer maxHeightPx
    ) {
        if (isInvalidPlaceResourcePath(placeId) || isInvalidPlaceResourcePath(photoReference)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        int safeMaxWidthPx = normalizePhotoSize(maxWidthPx);
        int safeMaxHeightPx = normalizePhotoSize(maxHeightPx);
        String cacheKey = buildPhotoMediaCacheKey(placeId, photoReference, safeMaxWidthPx, safeMaxHeightPx);
        PlacePhotoMediaResponse cachedMedia = googlePlacesCacheService.getPhotoMedia(cacheKey);
        if (cachedMedia != null) {
            return cachedMedia;
        }

        GooglePhotoMedia media = googlePlacesClient.getPhotoMedia(placeId, photoReference, safeMaxWidthPx, safeMaxHeightPx);
        PlacePhotoMediaResponse response = new PlacePhotoMediaResponse(media.photoUri());
        googlePlacesCacheService.cachePhotoMedia(cacheKey, response);

        return response;
    }

    private String buildSearchCacheKey(RecommendationCriteria criteria, String includedType, String defaultQuery) {
        return SEARCH_CACHE_KEY_PREFIX
                + includedType + ":"
                + normalize(criteria.effectivePreference(defaultQuery)) + ":"
                + criteria.originType() + ":"
                + normalize(criteria.originText()) + ":"
                + criteria.latitude() + ":"
                + criteria.longitude() + ":"
                + criteria.transportMode() + ":"
                + criteria.maxDistanceMeters() + ":"
                + criteria.maxTravelMinutes() + ":"
                + criteria.visitDateTime();
    }

    private String buildDetailCacheKey(String placeId, OffsetDateTime visitDateTime) {
        return DETAIL_CACHE_KEY_PREFIX + placeId + ":" + visitDateTime;
    }

    private String buildPhotoMediaCacheKey(String placeId, String photoReference, int maxWidthPx, int maxHeightPx) {
        return PHOTO_MEDIA_CACHE_KEY_PREFIX + placeId + ":" + photoReference + ":" + maxWidthPx + ":" + maxHeightPx;
    }

    private int normalizePhotoSize(Integer value) {
        if (value == null) {
            return 600;
        }

        return Math.max(1, Math.min(4800, value));
    }

    private boolean isInvalidPlaceResourcePath(String value) {
        return value == null || value.isBlank() || value.contains("/");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private PlaceRecommendationItem toRecommendationItem(RecommendationCriteria criteria, GooglePlace place, String includedType) {
        Integer distanceMeters = calculateDistanceMeters(criteria, place);
        OpenStatusResult openStatus = placeOpenStatusResolver.resolve(place.regularOpeningHours(), criteria.visitDateTime());
        double score = placeRecommendationScorer.calculate(
                criteria,
                place.rating(),
                place.userRatingCount(),
                distanceMeters,
                openStatus,
                place.primaryType(),
                includedType
        );
        String reason = buildReason(criteria, place, distanceMeters, openStatus);

        return new PlaceRecommendationItem(
                place.id(),
                place.displayName().text(),
                resolveCategory(place),
                place.rating(),
                place.userRatingCount(),
                place.formattedAddress(),
                place.regularOpeningHours() == null ? null : place.regularOpeningHours().openNow(),
                openStatus.status(),
                openStatus.reason(),
                openStatus.needsVerification(),
                place.location() == null ? null : place.location().latitude(),
                place.location() == null ? null : place.location().longitude(),
                distanceMeters,
                place.googleMapsUri(),
                buildMapPreview(place),
                reason,
                score
        );
    }

    private PlaceDetailResponse toPlaceDetailResponse(GooglePlace place, OffsetDateTime visitDateTime) {
        OpenStatusResult openStatus = placeOpenStatusResolver.resolve(place.regularOpeningHours(), visitDateTime);

        return new PlaceDetailResponse(
                place.id(),
                place.displayName().text(),
                resolveCategory(place),
                place.rating(),
                place.userRatingCount(),
                place.formattedAddress(),
                place.regularOpeningHours() == null ? null : place.regularOpeningHours().openNow(),
                openStatus.status(),
                openStatus.reason(),
                openStatus.needsVerification(),
                place.location() == null ? null : place.location().latitude(),
                place.location() == null ? null : place.location().longitude(),
                place.googleMapsUri(),
                buildMapPreview(place),
                place.websiteUri(),
                place.nationalPhoneNumber(),
                place.regularOpeningHours() == null ? List.of() : nullToEmpty(place.regularOpeningHours().weekdayDescriptions()),
                place.photos() == null ? List.of() : place.photos().stream()
                        .map(photo -> new PlacePhotoResponse(photo.name(), extractPhotoReference(photo.name()), photo.widthPx(), photo.heightPx()))
                        .toList()
        );
    }

    private String extractPhotoReference(String photoName) {
        if (photoName == null || photoName.isBlank()) {
            return null;
        }

        String marker = "/photos/";
        int index = photoName.lastIndexOf(marker);
        if (index < 0) {
            return photoName;
        }

        return photoName.substring(index + marker.length());
    }

    private MapPreviewResponse buildMapPreview(GooglePlace place) {
        Double lat = place.location() == null ? null : place.location().latitude();
        Double lng = place.location() == null ? null : place.location().longitude();
        boolean previewAvailable = place.id() != null && lat != null && lng != null;

        return new MapPreviewResponse(
                "GOOGLE_MAPS",
                place.id(),
                lat,
                lng,
                place.googleMapsUri(),
                previewAvailable
        );
    }

    private List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private boolean isWithinRequestedWalkingDistance(RecommendationCriteria criteria, PlaceRecommendationItem item) {
        if (criteria.transportMode() != TransportMode.WALK) {
            return true;
        }
        if (criteria.maxDistanceMeters() == null || item.distanceMeters() == null) {
            return true;
        }
        return item.distanceMeters() <= criteria.maxDistanceMeters();
    }

    private String resolveCategory(GooglePlace place) {
        if (place.primaryTypeDisplayName() != null && place.primaryTypeDisplayName().text() != null) {
            return place.primaryTypeDisplayName().text();
        }
        return place.primaryType();
    }

    private String buildReason(
            RecommendationCriteria criteria,
            GooglePlace place,
            Integer distanceMeters,
            OpenStatusResult openStatus
    ) {
        StringBuilder reason = new StringBuilder();

        if (distanceMeters != null) {
            reason.append("기준 위치에서 약 ")
                    .append(distanceMeters)
                    .append("m 거리입니다. ");
        }

        if (place.rating() != null && place.userRatingCount() != null) {
            reason.append("Google 평점 ")
                    .append(place.rating())
                    .append("점, 리뷰 ")
                    .append(place.userRatingCount())
                    .append("개 기준으로 신뢰도를 판단했습니다. ");
        }

        if (openStatus != null && openStatus.status() != null) {
            reason.append(openStatus.reason());
        }

        if (reason.isEmpty()) {
            return criteria.effectivePreference("장소") + " 검색 조건과 위치 기준으로 찾은 후보입니다.";
        }

        return reason.toString().trim();
    }

    private Integer calculateDistanceMeters(RecommendationCriteria criteria, GooglePlace place) {
        if (criteria.latitude() == null || criteria.longitude() == null || place.location() == null) {
            return null;
        }

        return placeDistanceCalculator.calculateMeters(
                criteria.latitude(),
                criteria.longitude(),
                place.location().latitude(),
                place.location().longitude()
        );
    }

}
