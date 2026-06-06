package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.TransportMode;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.OpenStatus;
import com.devcrew.togetherpay.domain.openChatAI.service.PlaceOpenStatusResolver.OpenStatusResult;
import org.springframework.stereotype.Component;

@Component
public class PlaceRecommendationScorer {

    public double calculate(
            RecommendationCriteria criteria,
            Double rating,
            Integer reviewCount,
            Integer distanceMeters,
            OpenStatusResult openStatus,
            String primaryType,
            String includedType
    ) {
        double ratingScore = rating == null ? 0 : rating * 20;
        double reviewScore = reviewCount == null ? 0 : Math.min(20, Math.log10(reviewCount + 1) * 6);
        double distanceScore = calculateDistanceScore(criteria, distanceMeters);
        double openScore = calculateOpenScore(openStatus);
        double typeScore = calculateTypeScore(primaryType, includedType);
        double budgetScore = calculateBudgetScore(criteria, includedType);

        return ratingScore + reviewScore + distanceScore + openScore + typeScore + budgetScore;
    }

    private double calculateDistanceScore(RecommendationCriteria criteria, Integer distanceMeters) {
        if (distanceMeters == null) {
            return 0;
        }

        if (criteria.transportMode() == TransportMode.WALK) {
            int maxDistance = criteria.maxDistanceMeters() == null ? 1000 : criteria.maxDistanceMeters();
            double ratio = Math.min(1.0, distanceMeters / (double) maxDistance);
            return Math.max(0, 25 * (1 - ratio));
        }

        return Math.max(0, 15 - distanceMeters / 300.0);
    }

    private double calculateOpenScore(OpenStatusResult openStatus) {
        if (openStatus == null || openStatus.status() == null) {
            return 0;
        }

        return switch (openStatus.status()) {
            case LIKELY_OPEN -> 15;
            case UNKNOWN -> 3;
            case LIKELY_CLOSED -> -25;
        };
    }

    private double calculateTypeScore(String primaryType, String includedType) {
        if (primaryType == null) {
            return 0;
        }

        return includedType.equals(primaryType) ? 8 : 0;
    }

    private double calculateBudgetScore(RecommendationCriteria criteria, String includedType) {
        if ("tourist_attraction".equals(includedType) || criteria.budgetPerPerson() == null) {
            return 0;
        }

        if ("cafe".equals(includedType)) {
            return criteria.budgetPerPerson() >= 1000 ? 3 : -3;
        }

        if ("restaurant".equals(includedType)) {
            return criteria.budgetPerPerson() >= 1500 ? 3 : -3;
        }

        return 0;
    }
}
