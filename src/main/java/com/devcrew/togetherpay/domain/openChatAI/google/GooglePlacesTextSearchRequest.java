package com.devcrew.togetherpay.domain.openChatAI.google;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;

public record GooglePlacesTextSearchRequest(
        String textQuery,
        String languageCode,
        String regionCode,
        String includedType,
        Boolean strictTypeFiltering,
        Integer pageSize,
        GoogleLocationBias locationBias
) {
    public static GooglePlacesTextSearchRequest of(
            RecommendationCriteria criteria,
            String includedType,
            String defaultQuery
    ) {
        return new GooglePlacesTextSearchRequest(
                buildTextQuery(criteria, defaultQuery),
                "ko",
                null,
                includedType,
                false,
                10,
                GoogleLocationBias.from(criteria)
        );
    }

    private static String buildTextQuery(RecommendationCriteria criteria, String defaultQuery) {
        if (criteria.originText() == null || criteria.originText().isBlank()) {
            return criteria.effectivePreference(defaultQuery);
        }
        return criteria.effectivePreference(defaultQuery) + " " + criteria.originText();
    }
}
