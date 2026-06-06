package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.ChatIntent;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.OriginType;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RecommendationCriteriaValidator {

    public MissingCriteriaResult validate(ChatIntent intent, RecommendationCriteria criteria) {
        List<String> missingFields = new ArrayList<>();

        if (criteria == null || isBlank(criteria.effectivePreference(""))) {
            missingFields.add("preference");
        }
        if (criteria == null || criteria.originType() == null || !hasOrigin(criteria)) {
            missingFields.add("origin");
        }
        if (intent != ChatIntent.ATTRACTION_RECOMMENDATION && (criteria == null || criteria.budgetPerPerson() == null)) {
            missingFields.add("budgetPerPerson");
        }
        if (criteria == null || criteria.transportMode() == null) {
            missingFields.add("transportMode");
        }
        if (criteria == null || criteria.maxDistanceMeters() == null && criteria.maxTravelMinutes() == null) {
            missingFields.add("travelRange");
        }
        if (criteria == null || criteria.visitDateTime() == null) {
            missingFields.add("visitDateTime");
        }

        return new MissingCriteriaResult(missingFields, buildMessage(intent));
    }

    private boolean hasOrigin(RecommendationCriteria criteria) {
        if (criteria.originType() == OriginType.GPS) {
            return criteria.latitude() != null && criteria.longitude() != null;
        }

        if (criteria.originType() == OriginType.MANUAL_TEXT) {
            return !isBlank(criteria.originText());
        }

        return false;
    }

    private String buildMessage(ChatIntent intent) {
        if (intent == ChatIntent.CAFE_RECOMMENDATION) {
            return """
                    카페 추천을 위해 아래 조건이 필요합니다.
                    
                    - 카페 취향: 조용한 곳, 디저트가 좋은 곳, 작업하기 좋은 곳처럼 입력해도 됩니다.
                    - 기준 위치: 현재 위치, 일정 장소, 숙소 근처, 직접 입력 중 하나가 필요합니다.
                    - 1인 예산
                    - 이동수단: 도보 또는 대중교통
                    - 이동 허용 범위: 도보라면 거리, 대중교통이라면 소요 시간
                    - 방문 예정 시간
                    """;
        }

        if (intent == ChatIntent.ATTRACTION_RECOMMENDATION) {
            return """
                    관광지 추천을 위해 아래 조건이 필요합니다.
                    
                    - 관광지 취향: 실내, 야경, 쇼핑, 전통적인 명소처럼 입력해도 됩니다.
                    - 기준 위치: 현재 위치, 일정 장소, 숙소 근처, 직접 입력 중 하나가 필요합니다.
                    - 이동수단: 도보 또는 대중교통
                    - 이동 허용 범위: 도보라면 거리, 대중교통이라면 소요 시간
                    - 방문 예정 시간
                    """;
        }

        return """
                맛집 추천을 위해 아래 조건이 필요합니다.
                
                - 음식 취향: 라멘처럼 구체적으로 입력하거나, 매콤하고 덜 느끼한 음식처럼 추상적으로 입력해도 됩니다.
                - 기준 위치: 현재 위치, 일정 장소, 숙소 근처, 직접 입력 중 하나가 필요합니다.
                - 1인 예산
                - 이동수단: 도보 또는 대중교통
                - 이동 허용 범위: 도보라면 거리, 대중교통이라면 소요 시간
                - 방문 예정 시간
                """;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record MissingCriteriaResult(
            List<String> missingFields,
            String message
    ) {
        public boolean hasMissingFields() {
            return !missingFields.isEmpty();
        }
    }
}
