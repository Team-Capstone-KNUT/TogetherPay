package com.devcrew.togetherpay.domain.openChatAI.dto.request;

import jakarta.validation.constraints.Min;

import java.time.OffsetDateTime;

public record RecommendationCriteria(
        OriginType originType,
        Double latitude,
        Double longitude,
        String originText,
        String preference,
        String foodPreference,
        @Min(value = 1, message = "1인 예산은 1 이상이어야 합니다.")
        Integer budgetPerPerson,
        TransportMode transportMode,
        @Min(value = 1, message = "이동 가능 거리는 1 이상이어야 합니다.")
        Integer maxDistanceMeters,
        @Min(value = 1, message = "이동 가능 시간은 1 이상이어야 합니다.")
        Integer maxTravelMinutes,
        OffsetDateTime visitDateTime
) {
    public String effectivePreference(String fallback) {
        if (preference != null && !preference.isBlank()) {
            return preference;
        }
        if (foodPreference != null && !foodPreference.isBlank()) {
            return foodPreference;
        }
        return fallback;
    }
}
