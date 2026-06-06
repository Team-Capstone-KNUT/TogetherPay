package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.ChatIntent;
import com.devcrew.togetherpay.domain.openChatAI.TripSelectionContext;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatBlock;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.TripSelectionOption;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.domain.trip.repository.TripRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final TripRepository tripRepository;
    private final ChatIntentService chatIntentService;
    private final ChatConversationStateStore conversationStateStore;
    private final TravelContextService travelContextService;
    private final RecommendationOrchestrator recommendationOrchestrator;
    private final RecommendationCriteriaValidator recommendationCriteriaValidator;
    private final BudgetInsightService budgetInsightService;
    private final TripHealthScoreService tripHealthScoreService;

    public ChatResponse chat(Long userId, Long teamId, ChatRequest request) {
        String question = request.question();

        if (conversationStateStore.findPendingTripSelection(userId, teamId).isPresent()) {
            return handlerTripSelection(userId, teamId, question);
        }

        ChatIntent intent = chatIntentService.resolve(request);

        if (intent == ChatIntent.SCHEDULE_SUMMARY) {
            if (request.tripId() != null) {
                return summarizeSchedule(userId, teamId, request.tripId());
            }
            return askTripSelection(userId, teamId);
        }

        if (intent == ChatIntent.FOOD_RECOMMENDATION) {
            return handlePlaceRecommendation(userId, teamId, request, ChatIntent.FOOD_RECOMMENDATION);
        }

        if (intent == ChatIntent.CAFE_RECOMMENDATION) {
            return handlePlaceRecommendation(userId, teamId, request, ChatIntent.CAFE_RECOMMENDATION);
        }

        if (intent == ChatIntent.ATTRACTION_RECOMMENDATION) {
            return handlePlaceRecommendation(userId, teamId, request, ChatIntent.ATTRACTION_RECOMMENDATION);
        }

        if (intent == ChatIntent.BUDGET_INSIGHT) {
            return handleBudgetInsight(userId, teamId, request);
        }

        if (intent == ChatIntent.TRIP_HEALTH_SCORE) {
            return handleTripHealthScore(userId, teamId, request);
        }

        if (intent == ChatIntent.TRIP_HEALTH_IMPROVEMENT) {
            return handleTripHealthImprovement(userId, teamId, request);
        }

        String answer = chatClient.prompt()
                .user(request.question())
                .call()
                .content();

        return ChatResponse.text(answer);
    }

    private ChatResponse askTripSelection(Long userId, Long teamId) {
        List<Trip> trips = tripRepository.findByTeamIdOrderByStartDateAsc(teamId);

        if (trips.isEmpty()) {
            return ChatResponse.text("등록된 여행이 없습니다.");
        }

        List<Long> tripIds = trips.stream()
                .map(Trip::getId)
                .toList();

        conversationStateStore.savePendingTripSelection(userId, new TripSelectionContext(teamId, tripIds));

        StringBuilder sb = new StringBuilder();
        sb.append("요약할 여행을 선택해주세요.\n\n");

        List<TripSelectionOption> options = new ArrayList<>();
        int i = 0;
        while(i < trips.size()) {
            Trip trip = trips.get(i);
            int number = i + 1;

            options.add(new TripSelectionOption(number, trip.getId(), trip.getTitle()));

            sb.append(number)
                    .append(". ")
                    .append(trip.getTitle())
                    .append(" (id ")
                    .append(trip.getId())
                    .append(")")
                    .append("\n");

            i++;
        }

        sb.append("\n번호를 입력해주세요.");

        return new ChatResponse(
                sb.toString(),
                List.of(new ChatBlock("trip_selection_options", options))
        );
    }

    private ChatResponse handlerTripSelection(Long userId, Long teamId, String question) {
        TripSelectionContext context = conversationStateStore.findPendingTripSelection(userId, teamId)
                .orElseThrow();

        int selectedNumber;

        try {
            selectedNumber = Integer.parseInt(question.trim());
        } catch (NumberFormatException e) {
            return ChatResponse.text("번호로 입력해주세요.");
        }

        if (selectedNumber < 1 || selectedNumber > context.tripIds().size()) {
            return ChatResponse.text("선택 가능한 번호가 아닙니다. 다시 입력해주세요.");
        }

        Long selectedTripId = context.tripIds().get(selectedNumber - 1);

        conversationStateStore.clearPendingTripSelection(userId, context.teamId());

        return summarizeSchedule(userId, context.teamId(), selectedTripId);
    }

    private ChatResponse summarizeSchedule(Long userId, Long teamId, long tripId) {
        validateTripAccess(userId, teamId, tripId);

        String scheduleContext = travelContextService.buildScheduleSummaryContext(tripId);
        String prompt = """
            너는 여행 정산 서비스 TogetherPay의 AI '루루'야.
            사용자가 등록한 여행 일정만 기반으로 여행을 요약해줘.
            없는 장소, 시간, 비용, 교통편은 절대 지어내지 마.
    
            단순히 일정을 반복하지 말고, 사용자가 실제로 도움이 된다고 느낄 정보를 정리해줘.
    
            반드시 아래 형식으로 답변해:
    
            1. 루루의 한눈에 보는 여행 흐름
            - 전체 일정이 어떤 흐름인지 2~4문장으로 설명해.
            - 도시 이동, 쇼핑, 식사, 휴식, 공항 일정 같은 큰 흐름을 짚어줘.
    
            2. 날짜별 일정 정리
            - 날짜별로 제목과 설명을 자연스럽게 요약해.
            - 각 날짜마다 "이 날의 포인트"를 한 줄로 덧붙여줘.
    
            3. 루루 체크
            - 일정상 사용자가 확인하면 좋을 점을 2~5개 알려줘.
            - 이동 수단, 숙소, 예약, 공항 도착 시간, 준비물, 예산 분배 같은 관점에서 봐줘.
            - 단, 등록된 일정에서 추론 가능한 범위 안에서만 말해.
    
            4. 빠진 정보
            - 일정에 시간, 장소 주소, 이동 수단, 예약 정보, 예산 정보가 부족해 보이면 알려줘.
            - 확실하지 않은 내용은 "~가 등록되어 있지 않다면 추가해두면 좋아요"처럼 표현해.
    
            답변 톤:
            - 친근하지만 너무 장난스럽지 않게.
            - 한국어로 답변해.
            - 너무 길지 않게, 모바일 화면에서 읽기 좋게.
            - 문단 사이를 띄워서 읽기 쉽게.
    
            등록된 일정:
            %s
        """.formatted(scheduleContext);

        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        return ChatResponse.text(answer);
    }

    private void validateTripAccess(Long userId, Long teamId, Long tripId) {
        boolean hasAccess = tripRepository.existsByIdAndTeam_IdAndTeam_TeamUsers_User_Id(tripId, teamId, userId);

        if (!hasAccess) {
            throw new BusinessException(ErrorCode.NOT_A_TRIP_USER);
        }
    }

    private ChatResponse handleBudgetInsight(Long userId, Long teamId, ChatRequest request) {
        if (request.tripId() == null) {
            return new ChatResponse(
                    "예산 체크를 위해 여행을 선택해주세요.",
                    List.of(new ChatBlock("missing_budget_insight_trip", Map.of("fields", List.of("tripId"))))
            );
        }

        validateTripAccess(userId, teamId, request.tripId());

        return budgetInsightService.analyze(request.tripId(), request.question());
    }

    private ChatResponse handleTripHealthScore(Long userId, Long teamId, ChatRequest request) {
        if (request.tripId() == null) {
            return new ChatResponse(
                    "여행 건강도를 계산하려면 여행을 선택해주세요.",
                    List.of(new ChatBlock("missing_trip_health_score_trip", Map.of("fields", List.of("tripId"))))
            );
        }

        validateTripAccess(userId, teamId, request.tripId());

        return tripHealthScoreService.analyze(request.tripId(), request.question());
    }

    private ChatResponse handleTripHealthImprovement(Long userId, Long teamId, ChatRequest request) {
        if (request.tripId() == null) {
            return new ChatResponse(
                    "여행 건강도 개선안을 만들려면 여행을 선택해주세요.",
                    List.of(new ChatBlock("missing_trip_health_improvement_trip", Map.of("fields", List.of("tripId"))))
            );
        }

        if (request.targetScore() == null) {
            return new ChatResponse(
                    "목표 점수를 선택해주세요.",
                    List.of(new ChatBlock("missing_trip_health_improvement_target", Map.of("fields", List.of("targetScore"))))
            );
        }

        validateTripAccess(userId, teamId, request.tripId());

        return tripHealthScoreService.improve(request.tripId(), request.question(), request.targetScore());
    }

    private ChatResponse handlePlaceRecommendation(Long userId, Long teamId, ChatRequest request, ChatIntent intent) {
        RecommendationCriteria criteria = request.criteria();
        RecommendationCriteriaValidator.MissingCriteriaResult missingCriteria =
                recommendationCriteriaValidator.validate(intent, criteria);

        if (missingCriteria.hasMissingFields()) {
            return new ChatResponse(
                    missingCriteria.message(),
                    List.of(new ChatBlock("missing_recommendation_criteria", Map.of("fields", missingCriteria.missingFields())))
            );
        }

        String scheduleContext = null;
        if (request.tripId() != null) {
            validateTripAccess(userId, teamId, request.tripId());
            scheduleContext = travelContextService.buildScheduleSummaryContext(request.tripId());
        }

        if (intent == ChatIntent.CAFE_RECOMMENDATION) {
            return recommendationOrchestrator.recommendCafe(request, scheduleContext);
        }

        if (intent == ChatIntent.ATTRACTION_RECOMMENDATION) {
            return recommendationOrchestrator.recommendAttraction(request, scheduleContext);
        }

        return recommendationOrchestrator.recommendFood(request, scheduleContext);
    }

}
