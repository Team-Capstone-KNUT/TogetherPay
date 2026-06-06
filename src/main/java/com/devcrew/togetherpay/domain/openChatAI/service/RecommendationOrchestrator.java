package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatBlock;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlaceRecommendationItem;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlaceRecommendationResponse;
import com.devcrew.togetherpay.global.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationOrchestrator {

    private static final int MAX_RESPONSE_SIZE = 5;
    private static final Duration INSIGHT_CACHE_TTL = Duration.ofMinutes(10);
    private static final String INSIGHT_CACHE_KEY_PREFIX = "places:insight:";

    private final GooglePlacesService googlePlacesService;
    private final ChatClient chatClient;
    private final RedisService redisService;

    public ChatResponse recommendFood(ChatRequest request, String scheduleContext) {
        return recommendPlaces(
                request.criteria(),
                scheduleContext,
                "맛집",
                "음식 취향",
                googlePlacesService.searchRestaurants(request.criteria())
        );
    }

    public ChatResponse recommendCafe(ChatRequest request, String scheduleContext) {
        return recommendPlaces(
                request.criteria(),
                scheduleContext,
                "카페",
                "카페 취향",
                googlePlacesService.searchCafes(request.criteria())
        );
    }

    public ChatResponse recommendAttraction(ChatRequest request, String scheduleContext) {
        return recommendPlaces(
                request.criteria(),
                scheduleContext,
                "관광지",
                "관광지 취향",
                googlePlacesService.searchAttractions(request.criteria())
        );
    }

    private ChatResponse recommendPlaces(
            RecommendationCriteria criteria,
            String scheduleContext,
            String domainName,
            String preferenceLabel,
            List<PlaceRecommendationItem> candidates
    ) {
        List<PlaceRecommendationItem> places = candidates.stream()
                .sorted(Comparator.comparingDouble(PlaceRecommendationItem::score).reversed())
                .limit(MAX_RESPONSE_SIZE)
                .toList();

        if (places.isEmpty()) {
            return ChatResponse.text("조건에 맞는 " + domainName + " 후보를 찾지 못했습니다. 기준 위치나 이동 범위를 넓혀서 다시 시도해주세요.");
        }

        String message = buildInsightMessage(criteria, places, scheduleContext, domainName, preferenceLabel);

        return new ChatResponse(
                message,
                List.of(new ChatBlock("place_card_list", new PlaceRecommendationResponse(places)))
        );
    }

    private String buildMessage(RecommendationCriteria criteria, int placeCount) {
        return "%s 기준으로 조건에 맞는 후보 %d곳을 찾았습니다."
                .formatted(criteria.effectivePreference("장소"), placeCount);
    }

    private String buildInsightMessage(
            RecommendationCriteria criteria,
            List<PlaceRecommendationItem> places,
            String scheduleContext,
            String domainName,
            String preferenceLabel
    ) {
        String cacheKey = buildInsightCacheKey(criteria, places, scheduleContext, domainName);
        String cachedMessage = redisService.getValues(cacheKey);
        if (cachedMessage != null && !cachedMessage.isBlank()) {
            return cachedMessage;
        }

        String prompt = """
                너는 여행 정산 서비스 TogetherPay의 AI '루루'야.
                아래 Google Places 기반 %s 후보를 보고 사용자에게 짧은 추천 인사이트를 제공해.
                
                규칙:
                - 장소 존재 여부, 평점, 리뷰 수, 거리, 영업 여부는 제공된 데이터만 사용해.
                - 제공되지 않은 가격, 메뉴, 예약 가능 여부, 라스트오더는 절대 지어내지 마.
                - 일정 정보가 있으면 동선 관점에서만 조심스럽게 설명해.
                - 3~5문장으로 모바일에서 읽기 좋게 답변해.
                - 한국어로 답변해.
                
                사용자 조건:
                %s: %s
                기준 위치: %s
                이동수단: %s
                이동 가능 거리(m): %s
                이동 가능 시간(분): %s
                방문 예정 시간: %s
                
                일정 정보:
                %s
                
                후보:
                %s
                """.formatted(
                domainName,
                preferenceLabel,
                criteria.effectivePreference(domainName),
                criteria.originText(),
                criteria.transportMode(),
                criteria.maxDistanceMeters(),
                criteria.maxTravelMinutes(),
                criteria.visitDateTime(),
                scheduleContext == null ? "일정 정보 없음" : scheduleContext,
                formatPlacesForPrompt(places)
        );

        String answer;

        try {
            answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            log.warn("OpenAI recommendation insight failed. message={}", e.getMessage());
            return buildMessage(criteria, places.size());
        }

        if (answer == null || answer.isBlank()) {
            return buildMessage(criteria, places.size());
        }

        redisService.setValues(cacheKey, answer, INSIGHT_CACHE_TTL);

        return answer;
    }

    private String buildInsightCacheKey(
            RecommendationCriteria criteria,
            List<PlaceRecommendationItem> places,
            String scheduleContext,
            String domainName
    ) {
        return INSIGHT_CACHE_KEY_PREFIX
                + normalize(domainName) + ":"
                + normalize(criteria.effectivePreference(domainName)) + ":"
                + normalize(criteria.originText()) + ":"
                + criteria.transportMode() + ":"
                + criteria.maxDistanceMeters() + ":"
                + criteria.maxTravelMinutes() + ":"
                + criteria.visitDateTime() + ":"
                + sha256(normalize(scheduleContext)) + ":"
                + places.stream()
                .map(PlaceRecommendationItem::placeId)
                .collect(Collectors.joining(","));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private String formatPlacesForPrompt(List<PlaceRecommendationItem> places) {
        StringBuilder sb = new StringBuilder();

        for (PlaceRecommendationItem place : places) {
            sb.append("- ")
                    .append(place.name())
                    .append(" / 카테고리: ")
                    .append(place.category())
                    .append(" / 평점: ")
                    .append(place.rating())
                    .append(" / 리뷰 수: ")
                    .append(place.reviewCount())
                    .append(" / 거리(m): ")
                    .append(place.distanceMeters())
                    .append(" / 영업 판단: ")
                    .append(place.openStatus())
                    .append(" / 확인 필요: ")
                    .append(place.needsVerification())
                    .append("\n");
        }

        return sb.toString();
    }
}
