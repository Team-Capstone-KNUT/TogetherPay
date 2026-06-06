package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.OriginType;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.TransportMode;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatBlock;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.MapPreviewResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.OpenStatus;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlaceRecommendationItem;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlaceRecommendationResponse;
import com.devcrew.togetherpay.global.redis.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationOrchestratorTest {

    private final GooglePlacesService googlePlacesService = mock(GooglePlacesService.class);
    private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final RedisService redisService = mock(RedisService.class);
    private final RecommendationOrchestrator orchestrator = new RecommendationOrchestrator(
            googlePlacesService,
            chatClient,
            redisService
    );

    @Test
    void recommendsFoodWithPlaceCardListBlock() {
        ChatRequest request = request("라멘");
        when(googlePlacesService.searchRestaurants(request.criteria())).thenReturn(List.of(
                place("place-low", "낮은 점수", 70.0),
                place("place-high", "높은 점수", 95.0)
        ));
        when(redisService.getValues(anyString())).thenReturn(null);
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("동선상 높은 점수 장소가 적합합니다.");

        ChatResponse response = orchestrator.recommendFood(request, "1일차 저녁 신주쿠");

        assertThat(response.message()).isEqualTo("동선상 높은 점수 장소가 적합합니다.");
        assertThat(response.blocks()).hasSize(1);

        ChatBlock block = response.blocks().get(0);
        assertThat(block.type()).isEqualTo("place_card_list");
        assertThat(block.data()).isInstanceOf(PlaceRecommendationResponse.class);

        PlaceRecommendationResponse data = (PlaceRecommendationResponse) block.data();
        assertThat(data.places())
                .extracting(PlaceRecommendationItem::placeId)
                .containsExactly("place-high", "place-low");
        verify(redisService).setValues(anyString(), eq("동선상 높은 점수 장소가 적합합니다."), any());
    }

    @Test
    void returnsTextResponseWhenNoFoodCandidateExists() {
        ChatRequest request = request("라멘");
        when(googlePlacesService.searchRestaurants(request.criteria())).thenReturn(List.of());

        ChatResponse response = orchestrator.recommendFood(request, "1일차 저녁 신주쿠");

        assertThat(response.message()).contains("조건에 맞는 맛집 후보를 찾지 못했습니다");
        assertThat(response.blocks()).isEmpty();
        verify(redisService, never()).getValues(anyString());
        verify(chatClient, never()).prompt();
    }

    @Test
    void fallsBackToDefaultMessageWhenOpenAiFails() {
        ChatRequest request = request("매콤하고 덜 느끼한 음식");
        when(googlePlacesService.searchRestaurants(request.criteria())).thenReturn(List.of(place("place-1", "라멘집", 88.0)));
        when(redisService.getValues(anyString())).thenReturn(null);
        when(chatClient.prompt().user(anyString()).call().content()).thenThrow(new RuntimeException("openai failed"));

        ChatResponse response = orchestrator.recommendFood(request, "1일차 저녁 신주쿠");

        assertThat(response.message()).isEqualTo("매콤하고 덜 느끼한 음식 기준으로 조건에 맞는 후보 1곳을 찾았습니다.");
        assertThat(response.blocks()).hasSize(1);
        verify(redisService, never()).setValues(anyString(), anyString(), any());
    }

    @Test
    void recommendsCafeThroughCafeSearch() {
        ChatRequest request = request("조용한 카페");
        when(googlePlacesService.searchCafes(request.criteria())).thenReturn(List.of(place("cafe-1", "카페", 80.0)));
        when(redisService.getValues(anyString())).thenReturn("카페 인사이트");

        ChatResponse response = orchestrator.recommendCafe(request, "2일차 오후");

        assertThat(response.message()).isEqualTo("카페 인사이트");
        assertThat(response.blocks()).hasSize(1);
        verify(googlePlacesService).searchCafes(request.criteria());
        verify(googlePlacesService, never()).searchRestaurants(request.criteria());
    }

    @Test
    void recommendsAttractionThroughAttractionSearch() {
        ChatRequest request = request("실내 명소");
        when(googlePlacesService.searchAttractions(request.criteria())).thenReturn(List.of(place("spot-1", "미술관", 82.0)));
        when(redisService.getValues(anyString())).thenReturn("관광지 인사이트");

        ChatResponse response = orchestrator.recommendAttraction(request, "비 오는 날 오전");

        assertThat(response.message()).isEqualTo("관광지 인사이트");
        assertThat(response.blocks()).hasSize(1);
        verify(googlePlacesService).searchAttractions(request.criteria());
        verify(googlePlacesService, never()).searchRestaurants(request.criteria());
    }

    private ChatRequest request(String preference) {
        RecommendationCriteria criteria = new RecommendationCriteria(
                OriginType.MANUAL_TEXT,
                35.6938,
                139.7034,
                "도쿄 신주쿠 가부키초",
                preference,
                null,
                3000,
                TransportMode.WALK,
                1200,
                20,
                OffsetDateTime.parse("2026-06-10T19:00:00+09:00")
        );

        return new ChatRequest("추천해줘", 1L, "FOOD_RECOMMENDATION", criteria);
    }

    private PlaceRecommendationItem place(String placeId, String name, double score) {
        return new PlaceRecommendationItem(
                placeId,
                name,
                "restaurant",
                4.3,
                120,
                "Tokyo",
                true,
                OpenStatus.LIKELY_OPEN,
                "방문 예정 시간에 영업 가능성이 높습니다.",
                false,
                35.6938,
                139.7034,
                300,
                "https://maps.google.com/?cid=" + placeId,
                new MapPreviewResponse(
                        "GOOGLE_MAPS",
                        placeId,
                        35.6938,
                        139.7034,
                        "https://maps.google.com/?cid=" + placeId,
                        true
                ),
                null,
                score
        );
    }
}
