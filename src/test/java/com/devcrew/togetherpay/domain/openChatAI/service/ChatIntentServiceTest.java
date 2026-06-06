package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.ChatIntent;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatIntentServiceTest {

    private final ChatIntentService chatIntentService = new ChatIntentService();

    @Test
    void resolvesBudgetInsightByExplicitIntent() {
        ChatRequest request = new ChatRequest("아무 문장", 1L, "BUDGET_INSIGHT", null);

        ChatIntent result = chatIntentService.resolve(request);

        assertThat(result).isEqualTo(ChatIntent.BUDGET_INSIGHT);
    }

    @Test
    void resolvesBudgetInsightByBudgetKeywords() {
        ChatRequest request = new ChatRequest("이번 여행 남은 예산 괜찮아?", 1L, null, null);

        ChatIntent result = chatIntentService.resolve(request);

        assertThat(result).isEqualTo(ChatIntent.BUDGET_INSIGHT);
    }

    @Test
    void resolvesTripHealthScoreByExplicitIntent() {
        ChatRequest request = new ChatRequest("아무 문장", 1L, "TRIP_HEALTH_SCORE", null);

        ChatIntent result = chatIntentService.resolve(request);

        assertThat(result).isEqualTo(ChatIntent.TRIP_HEALTH_SCORE);
    }

    @Test
    void resolvesTripHealthScoreByKeywords() {
        ChatRequest request = new ChatRequest("이번 여행 건강도 몇 점이야?", 1L, null, null);

        ChatIntent result = chatIntentService.resolve(request);

        assertThat(result).isEqualTo(ChatIntent.TRIP_HEALTH_SCORE);
    }

    @Test
    void resolvesTripHealthImprovementByExplicitIntent() {
        ChatRequest request = new ChatRequest("90점으로 개선해줘", 1L, "TRIP_HEALTH_IMPROVEMENT", null, 90);

        ChatIntent result = chatIntentService.resolve(request);

        assertThat(result).isEqualTo(ChatIntent.TRIP_HEALTH_IMPROVEMENT);
    }

    @Test
    void resolvesTripHealthImprovementByKeywords() {
        ChatRequest request = new ChatRequest("90점 만들기", 1L, null, null, 90);

        ChatIntent result = chatIntentService.resolve(request);

        assertThat(result).isEqualTo(ChatIntent.TRIP_HEALTH_IMPROVEMENT);
    }
}
