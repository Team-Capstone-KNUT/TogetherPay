package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.OriginType;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.TransportMode;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.OpenStatus;
import com.devcrew.togetherpay.domain.openChatAI.service.PlaceOpenStatusResolver.OpenStatusResult;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceRecommendationScorerTest {

    private final PlaceRecommendationScorer scorer = new PlaceRecommendationScorer();

    @Test
    void likelyOpenPlaceScoresHigherThanLikelyClosedPlace() {
        RecommendationCriteria criteria = walkingCriteria(3000);

        double openScore = scorer.calculate(
                criteria,
                4.3,
                1000,
                300,
                new OpenStatusResult(OpenStatus.LIKELY_OPEN, "", false),
                "restaurant",
                "restaurant"
        );
        double closedScore = scorer.calculate(
                criteria,
                4.3,
                1000,
                300,
                new OpenStatusResult(OpenStatus.LIKELY_CLOSED, "", true),
                "restaurant",
                "restaurant"
        );

        assertThat(openScore).isGreaterThan(closedScore);
    }

    @Test
    void closerWalkingPlaceScoresHigher() {
        RecommendationCriteria criteria = walkingCriteria(3000);

        double nearScore = scorer.calculate(criteria, 4.0, 500, 100, null, "restaurant", "restaurant");
        double farScore = scorer.calculate(criteria, 4.0, 500, 900, null, "restaurant", "restaurant");

        assertThat(nearScore).isGreaterThan(farScore);
    }

    @Test
    void primaryTypeMatchAddsScore() {
        RecommendationCriteria criteria = walkingCriteria(3000);

        double matchedScore = scorer.calculate(criteria, 4.0, 500, 300, null, "cafe", "cafe");
        double unmatchedScore = scorer.calculate(criteria, 4.0, 500, 300, null, "restaurant", "cafe");

        assertThat(matchedScore).isGreaterThan(unmatchedScore);
    }

    @Test
    void lowRestaurantBudgetIsPenalized() {
        double enoughBudgetScore = scorer.calculate(walkingCriteria(3000), 4.0, 500, 300, null, "restaurant", "restaurant");
        double lowBudgetScore = scorer.calculate(walkingCriteria(1000), 4.0, 500, 300, null, "restaurant", "restaurant");

        assertThat(enoughBudgetScore).isGreaterThan(lowBudgetScore);
    }

    @Test
    void attractionDoesNotUseBudgetScore() {
        double enoughBudgetScore = scorer.calculate(walkingCriteria(3000), 4.0, 500, 300, null, "tourist_attraction", "tourist_attraction");
        double noBudgetScore = scorer.calculate(walkingCriteria(null), 4.0, 500, 300, null, "tourist_attraction", "tourist_attraction");

        assertThat(enoughBudgetScore).isEqualTo(noBudgetScore);
    }

    private RecommendationCriteria walkingCriteria(Integer budgetPerPerson) {
        return new RecommendationCriteria(
                OriginType.GPS,
                35.0,
                139.0,
                "신주쿠",
                "라멘",
                null,
                budgetPerPerson,
                TransportMode.WALK,
                1000,
                null,
                OffsetDateTime.parse("2026-06-02T19:30:00+09:00")
        );
    }
}
