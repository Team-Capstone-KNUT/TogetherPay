package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.openChatAI.ChatIntent;
import com.devcrew.togetherpay.domain.openChatAI.TripSelectionContext;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.OriginType;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.TransportMode;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatBlock;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatResponse;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.domain.trip.repository.TripRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TEAM_ID = 10L;

    private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final TripRepository tripRepository = mock(TripRepository.class);
    private final ChatIntentService chatIntentService = mock(ChatIntentService.class);
    private final ChatConversationStateStore conversationStateStore = mock(ChatConversationStateStore.class);
    private final TravelContextService travelContextService = mock(TravelContextService.class);
    private final RecommendationOrchestrator recommendationOrchestrator = mock(RecommendationOrchestrator.class);
    private final RecommendationCriteriaValidator recommendationCriteriaValidator = mock(RecommendationCriteriaValidator.class);
    private final BudgetInsightService budgetInsightService = mock(BudgetInsightService.class);
    private final TripHealthScoreService tripHealthScoreService = mock(TripHealthScoreService.class);
    private final ChatService chatService = new ChatService(
            chatClient,
            tripRepository,
            chatIntentService,
            conversationStateStore,
            travelContextService,
            recommendationOrchestrator,
            recommendationCriteriaValidator,
            budgetInsightService,
            tripHealthScoreService
    );

    @Test
    void asksTripSelectionWhenScheduleSummaryHasNoTripId() {
        ChatRequest request = new ChatRequest("일정 요약해줘", null, "SCHEDULE_SUMMARY", null);
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.SCHEDULE_SUMMARY);
        when(tripRepository.findByTeamIdOrderByStartDateAsc(TEAM_ID)).thenReturn(List.of(trip(11L, "도쿄"), trip(12L, "오사카")));

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response.message()).contains("요약할 여행을 선택해주세요");
        assertThat(response.blocks()).hasSize(1);

        ChatBlock block = response.blocks().get(0);
        assertThat(block.type()).isEqualTo("trip_selection_options");
        verify(conversationStateStore).savePendingTripSelection(USER_ID, new TripSelectionContext(TEAM_ID, List.of(11L, 12L)));
    }

    @Test
    void handlesPendingTripSelectionBeforeResolvingNewIntent() {
        ChatRequest request = new ChatRequest("1", null, null, null);
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID))
                .thenReturn(Optional.of(new TripSelectionContext(TEAM_ID, List.of(11L, 12L))));
        when(tripRepository.existsByIdAndTeam_IdAndTeam_TeamUsers_User_Id(11L, TEAM_ID, USER_ID)).thenReturn(true);
        when(travelContextService.buildScheduleSummaryContext(11L)).thenReturn("도쿄 일정");
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("요약 답변");

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response.message()).isEqualTo("요약 답변");
        verify(conversationStateStore).clearPendingTripSelection(USER_ID, TEAM_ID);
        verify(chatIntentService, never()).resolve(request);
    }

    @Test
    void returnsMissingCriteriaBlockWithoutCallingRecommendationOrTripContext() {
        ChatRequest request = new ChatRequest("맛집 추천", 11L, "FOOD_RECOMMENDATION", null);
        RecommendationCriteriaValidator.MissingCriteriaResult missing =
                new RecommendationCriteriaValidator.MissingCriteriaResult(List.of("origin", "visitDateTime"), "조건이 필요합니다.");
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.FOOD_RECOMMENDATION);
        when(recommendationCriteriaValidator.validate(ChatIntent.FOOD_RECOMMENDATION, null)).thenReturn(missing);

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response.message()).isEqualTo("조건이 필요합니다.");
        assertThat(response.blocks()).hasSize(1);
        assertThat(response.blocks().get(0).type()).isEqualTo("missing_recommendation_criteria");
        verify(tripRepository, never()).existsByIdAndTeam_IdAndTeam_TeamUsers_User_Id(11L, TEAM_ID, USER_ID);
        verify(recommendationOrchestrator, never()).recommendFood(request, null);
    }

    @Test
    void delegatesFoodRecommendationWithScheduleContextWhenTripIdExists() {
        RecommendationCriteria criteria = criteria();
        ChatRequest request = new ChatRequest("맛집 추천", 11L, "FOOD_RECOMMENDATION", criteria);
        ChatResponse expected = ChatResponse.text("추천 결과");
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.FOOD_RECOMMENDATION);
        when(recommendationCriteriaValidator.validate(ChatIntent.FOOD_RECOMMENDATION, criteria))
                .thenReturn(new RecommendationCriteriaValidator.MissingCriteriaResult(List.of(), "조건 없음"));
        when(tripRepository.existsByIdAndTeam_IdAndTeam_TeamUsers_User_Id(11L, TEAM_ID, USER_ID)).thenReturn(true);
        when(travelContextService.buildScheduleSummaryContext(11L)).thenReturn("도쿄 일정");
        when(recommendationOrchestrator.recommendFood(request, "도쿄 일정")).thenReturn(expected);

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response).isEqualTo(expected);
        verify(recommendationOrchestrator).recommendFood(request, "도쿄 일정");
    }

    @Test
    void throwsWhenTripAccessIsDeniedForRecommendationWithTripId() {
        RecommendationCriteria criteria = criteria();
        ChatRequest request = new ChatRequest("맛집 추천", 11L, "FOOD_RECOMMENDATION", criteria);
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.FOOD_RECOMMENDATION);
        when(recommendationCriteriaValidator.validate(ChatIntent.FOOD_RECOMMENDATION, criteria))
                .thenReturn(new RecommendationCriteriaValidator.MissingCriteriaResult(List.of(), "조건 없음"));
        when(tripRepository.existsByIdAndTeam_IdAndTeam_TeamUsers_User_Id(11L, TEAM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> chatService.chat(USER_ID, TEAM_ID, request))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_A_TRIP_USER));

        verify(travelContextService, never()).buildScheduleSummaryContext(11L);
        verify(recommendationOrchestrator, never()).recommendFood(request, null);
    }

    @Test
    void delegatesGeneralChatToOpenAi() {
        ChatRequest request = new ChatRequest("안녕", null, null, null);
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.GENERAL_CHAT);
        when(chatClient.prompt().user("안녕").call().content()).thenReturn("안녕하세요");

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response.message()).isEqualTo("안녕하세요");
        assertThat(response.blocks()).isEmpty();
    }

    @Test
    void returnsMissingBudgetTripBlockWhenBudgetInsightHasNoTripId() {
        ChatRequest request = new ChatRequest("이번 여행 예산 괜찮아?", null, "BUDGET_INSIGHT", null);
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.BUDGET_INSIGHT);

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response.message()).isEqualTo("예산 체크를 위해 여행을 선택해주세요.");
        assertThat(response.blocks()).hasSize(1);
        assertThat(response.blocks().get(0).type()).isEqualTo("missing_budget_insight_trip");
        verify(budgetInsightService, never()).analyze(11L, request.question());
    }

    @Test
    void delegatesBudgetInsightAfterTripAccessValidation() {
        ChatRequest request = new ChatRequest("이번 여행 예산 괜찮아?", 11L, "BUDGET_INSIGHT", null);
        ChatResponse expected = ChatResponse.text("예산 인사이트");
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.BUDGET_INSIGHT);
        when(tripRepository.existsByIdAndTeam_IdAndTeam_TeamUsers_User_Id(11L, TEAM_ID, USER_ID)).thenReturn(true);
        when(budgetInsightService.analyze(11L, request.question())).thenReturn(expected);

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response).isEqualTo(expected);
        verify(budgetInsightService).analyze(11L, request.question());
    }

    @Test
    void throwsWhenTripAccessIsDeniedForBudgetInsight() {
        ChatRequest request = new ChatRequest("이번 여행 예산 괜찮아?", 11L, "BUDGET_INSIGHT", null);
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.BUDGET_INSIGHT);
        when(tripRepository.existsByIdAndTeam_IdAndTeam_TeamUsers_User_Id(11L, TEAM_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> chatService.chat(USER_ID, TEAM_ID, request))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_A_TRIP_USER));

        verify(budgetInsightService, never()).analyze(11L, request.question());
    }

    @Test
    void returnsMissingTripHealthScoreTripBlockWhenTripIdIsMissing() {
        ChatRequest request = new ChatRequest("여행 건강도 알려줘", null, "TRIP_HEALTH_SCORE", null);
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.TRIP_HEALTH_SCORE);

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response.message()).isEqualTo("여행 건강도를 계산하려면 여행을 선택해주세요.");
        assertThat(response.blocks()).hasSize(1);
        assertThat(response.blocks().get(0).type()).isEqualTo("missing_trip_health_score_trip");
        verify(tripHealthScoreService, never()).analyze(11L, request.question());
    }

    @Test
    void delegatesTripHealthScoreAfterTripAccessValidation() {
        ChatRequest request = new ChatRequest("여행 건강도 알려줘", 11L, "TRIP_HEALTH_SCORE", null);
        ChatResponse expected = ChatResponse.text("건강도 결과");
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.TRIP_HEALTH_SCORE);
        when(tripRepository.existsByIdAndTeam_IdAndTeam_TeamUsers_User_Id(11L, TEAM_ID, USER_ID)).thenReturn(true);
        when(tripHealthScoreService.analyze(11L, request.question())).thenReturn(expected);

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response).isEqualTo(expected);
        verify(tripHealthScoreService).analyze(11L, request.question());
    }

    @Test
    void returnsMissingTripHealthImprovementTargetBlockWhenTargetScoreIsMissing() {
        ChatRequest request = new ChatRequest("90점으로 개선해줘", 11L, "TRIP_HEALTH_IMPROVEMENT", null);
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.TRIP_HEALTH_IMPROVEMENT);

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response.message()).isEqualTo("목표 점수를 선택해주세요.");
        assertThat(response.blocks()).hasSize(1);
        assertThat(response.blocks().get(0).type()).isEqualTo("missing_trip_health_improvement_target");
        verify(tripHealthScoreService, never()).improve(11L, request.question(), 90);
    }

    @Test
    void delegatesTripHealthImprovementAfterTripAccessValidation() {
        ChatRequest request = new ChatRequest("90점으로 개선해줘", 11L, "TRIP_HEALTH_IMPROVEMENT", null, 90);
        ChatResponse expected = ChatResponse.text("개선안 결과");
        when(conversationStateStore.findPendingTripSelection(USER_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(chatIntentService.resolve(request)).thenReturn(ChatIntent.TRIP_HEALTH_IMPROVEMENT);
        when(tripRepository.existsByIdAndTeam_IdAndTeam_TeamUsers_User_Id(11L, TEAM_ID, USER_ID)).thenReturn(true);
        when(tripHealthScoreService.improve(11L, request.question(), 90)).thenReturn(expected);

        ChatResponse response = chatService.chat(USER_ID, TEAM_ID, request);

        assertThat(response).isEqualTo(expected);
        verify(tripHealthScoreService).improve(11L, request.question(), 90);
    }

    private Trip trip(Long id, String title) {
        return Trip.builder()
                .id(id)
                .title(title)
                .startDate(LocalDate.of(2026, 6, 10))
                .endDate(LocalDate.of(2026, 6, 12))
                .baseCurrency(Currency.JPY)
                .build();
    }

    private RecommendationCriteria criteria() {
        return new RecommendationCriteria(
                OriginType.MANUAL_TEXT,
                35.6938,
                139.7034,
                "도쿄 신주쿠",
                "라멘",
                null,
                3000,
                TransportMode.WALK,
                1200,
                20,
                OffsetDateTime.parse("2026-06-10T19:00:00+09:00")
        );
    }
}
