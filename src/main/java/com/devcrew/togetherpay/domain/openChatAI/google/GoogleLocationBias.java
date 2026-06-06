package com.devcrew.togetherpay.domain.openChatAI.google;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;

public record GoogleLocationBias(GoogleCircle circle) {

    public static GoogleLocationBias from(RecommendationCriteria criteria) {
        if (criteria.latitude() == null || criteria.longitude() == null) {
            return null;
        }

        double radius = criteria.maxDistanceMeters() == null ? 1000.0 : criteria.maxDistanceMeters().doubleValue();
        return new GoogleLocationBias(new GoogleCircle(new GoogleCenter(criteria.latitude(), criteria.longitude()), radius));
    }
}
