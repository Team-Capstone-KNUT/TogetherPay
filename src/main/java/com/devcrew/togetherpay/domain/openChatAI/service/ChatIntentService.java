package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.ChatIntent;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import org.springframework.stereotype.Service;

@Service
public class ChatIntentService {

    public ChatIntent resolve(ChatRequest request) {
        if (request.intent() != null && !request.intent().isBlank()) {
            return parseIntent(request.intent());
        }

        String question = request.question();

        if (isTravelScheduleSummary(question)) {
            return ChatIntent.SCHEDULE_SUMMARY;
        }

        if (containsAny(question, "맛집", "밥집", "식당", "음식", "먹을", "저녁", "점심")) {
            return ChatIntent.FOOD_RECOMMENDATION;
        }

        if (containsAny(question, "카페", "커피", "디저트")) {
            return ChatIntent.CAFE_RECOMMENDATION;
        }

        if (containsAny(question, "관광지", "명소", "가볼만한", "가 볼만한")) {
            return ChatIntent.ATTRACTION_RECOMMENDATION;
        }

        if (containsAny(question, "예산", "지출", "소비", "남은 돈", "남은 금액", "돈 얼마나", "얼마나 썼")) {
            return ChatIntent.BUDGET_INSIGHT;
        }

        if (containsAny(question, "여행 건강도", "건강도", "여행 점수", "몇 점", "몇점", "점수")) {
            return ChatIntent.TRIP_HEALTH_SCORE;
        }

        if (containsAny(question, "점으로 개선", "점으로 올", "점 만들기", "개선해줘")) {
            return ChatIntent.TRIP_HEALTH_IMPROVEMENT;
        }

        if (containsAny(question, "인사이트", "개선", "보완", "추가하면", "동선")) {
            return ChatIntent.ITINERARY_INSIGHT;
        }

        return ChatIntent.GENERAL_CHAT;
    }

    private ChatIntent parseIntent(String intent) {
        try {
            return ChatIntent.valueOf(intent.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChatIntent.GENERAL_CHAT;
        }
    }

    private boolean isTravelScheduleSummary(String question) {
        boolean hasScheduleKeyword = containsAny(question, "일정", "여행 계획", "여행 일정");
        boolean hasSummaryKeyword = containsAny(question, "요약", "정리", "확인", "보여줘");

        return hasScheduleKeyword && hasSummaryKeyword;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
