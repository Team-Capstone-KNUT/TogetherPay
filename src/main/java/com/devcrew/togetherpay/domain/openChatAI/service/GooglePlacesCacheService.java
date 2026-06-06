package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlaceDetailResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlacePhotoMediaResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlaceRecommendationItem;
import com.devcrew.togetherpay.global.redis.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesCacheService {

    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration PHOTO_MEDIA_CACHE_TTL = Duration.ofMinutes(30);

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public List<PlaceRecommendationItem> getSearchResult(String cacheKey) {
        String value = redisService.getValues(cacheKey);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize Google Places search cache. key={}", cacheKey);
            redisService.deleteValues(cacheKey);
            return null;
        }
    }

    public void cacheSearchResult(String cacheKey, List<PlaceRecommendationItem> items) {
        try {
            redisService.setValues(cacheKey, objectMapper.writeValueAsString(items), SEARCH_CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize Google Places search cache. key={}", cacheKey);
        }
    }

    public PlaceDetailResponse getPlaceDetail(String cacheKey) {
        String value = redisService.getValues(cacheKey);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(value, PlaceDetailResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize Google Places detail cache. key={}", cacheKey);
            redisService.deleteValues(cacheKey);
            return null;
        }
    }

    public void cachePlaceDetail(String cacheKey, PlaceDetailResponse detail) {
        try {
            redisService.setValues(cacheKey, objectMapper.writeValueAsString(detail), DETAIL_CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize Google Places detail cache. key={}", cacheKey);
        }
    }

    public PlacePhotoMediaResponse getPhotoMedia(String cacheKey) {
        String value = redisService.getValues(cacheKey);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(value, PlacePhotoMediaResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize Google Places photo media cache. key={}", cacheKey);
            redisService.deleteValues(cacheKey);
            return null;
        }
    }

    public void cachePhotoMedia(String cacheKey, PlacePhotoMediaResponse response) {
        try {
            redisService.setValues(cacheKey, objectMapper.writeValueAsString(response), PHOTO_MEDIA_CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize Google Places photo media cache. key={}", cacheKey);
        }
    }
}
