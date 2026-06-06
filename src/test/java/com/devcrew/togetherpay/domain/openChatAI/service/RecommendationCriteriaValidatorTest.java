package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.ChatIntent;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.OriginType;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.TransportMode;
import com.devcrew.togetherpay.domain.openChatAI.service.RecommendationCriteriaValidator.MissingCriteriaResult;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationCriteriaValidatorTest {

    private final RecommendationCriteriaValidator validator = new RecommendationCriteriaValidator();

    @Test
    void requiresAllCoreFieldsWhenCriteriaIsMissing() {
        MissingCriteriaResult result = validator.validate(ChatIntent.FOOD_RECOMMENDATION, null);

        assertThat(result.missingFields())
                .containsExactly("preference", "origin", "budgetPerPerson", "transportMode", "travelRange", "visitDateTime");
        assertThat(result.hasMissingFields()).isTrue();
    }

    @Test
    void acceptsFoodRecommendationWhenRequiredFieldsExist() {
        RecommendationCriteria criteria = criteria("라멘", 3000);

        MissingCriteriaResult result = validator.validate(ChatIntent.FOOD_RECOMMENDATION, criteria);

        assertThat(result.missingFields()).isEmpty();
    }

    @Test
    void acceptsCafeRecommendationWithPreferenceFieldInsteadOfFoodPreference() {
        RecommendationCriteria criteria = new RecommendationCriteria(
                OriginType.GPS,
                35.0,
                139.0,
                "신주쿠",
                "조용한 카페",
                null,
                2000,
                TransportMode.WALK,
                800,
                null,
                OffsetDateTime.parse("2026-06-02T15:00:00+09:00")
        );

        MissingCriteriaResult result = validator.validate(ChatIntent.CAFE_RECOMMENDATION, criteria);

        assertThat(result.missingFields()).isEmpty();
    }

    @Test
    void attractionDoesNotRequireBudget() {
        RecommendationCriteria criteria = criteria("야경 명소", null);

        MissingCriteriaResult result = validator.validate(ChatIntent.ATTRACTION_RECOMMENDATION, criteria);

        assertThat(result.missingFields()).doesNotContain("budgetPerPerson");
        assertThat(result.missingFields()).isEmpty();
    }

    @Test
    void manualOriginRequiresOriginText() {
        RecommendationCriteria criteria = new RecommendationCriteria(
                OriginType.MANUAL_TEXT,
                null,
                null,
                null,
                "라멘",
                null,
                3000,
                TransportMode.WALK,
                800,
                null,
                OffsetDateTime.parse("2026-06-02T19:30:00+09:00")
        );

        MissingCriteriaResult result = validator.validate(ChatIntent.FOOD_RECOMMENDATION, criteria);

        assertThat(result.missingFields()).contains("origin");
    }

    private RecommendationCriteria criteria(String preference, Integer budgetPerPerson) {
        return new RecommendationCriteria(
                OriginType.GPS,
                35.0,
                139.0,
                "신주쿠",
                preference,
                null,
                budgetPerPerson,
                TransportMode.WALK,
                800,
                null,
                OffsetDateTime.parse("2026-06-02T19:30:00+09:00")
        );
    }
}
