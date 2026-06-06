package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.TripSelectionContext;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import com.devcrew.togetherpay.global.redis.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatConversationStateStore {

    private static final String TRIP_SELECTION_KEY_PREFIX = "chat:trip-selection:";
    private static final Duration TRIP_SELECTION_TTL = Duration.ofMinutes(10);

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public Optional<TripSelectionContext> findPendingTripSelection(Long userId, Long teamId) {
        String value = redisService.getValues(buildTripSelectionKey(userId, teamId));

        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, TripSelectionContext.class));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public void savePendingTripSelection(Long userId, TripSelectionContext context) {
        try {
            String value = objectMapper.writeValueAsString(context);
            redisService.setValues(buildTripSelectionKey(userId, context.teamId()), value, TRIP_SELECTION_TTL);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public void clearPendingTripSelection(Long userId, Long teamId) {
        redisService.deleteValues(buildTripSelectionKey(userId, teamId));
    }

    private String buildTripSelectionKey(Long userId, Long teamId) {
        return TRIP_SELECTION_KEY_PREFIX + userId + ":" + teamId;
    }
}
