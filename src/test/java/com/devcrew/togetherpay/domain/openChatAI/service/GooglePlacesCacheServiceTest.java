package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlacePhotoMediaResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlaceRecommendationItem;
import com.devcrew.togetherpay.global.redis.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GooglePlacesCacheServiceTest {

    private final RedisService redisService = mock(RedisService.class);
    private final GooglePlacesCacheService cacheService = new GooglePlacesCacheService(redisService, new ObjectMapper());

    @Test
    void returnsNullWhenSearchCacheMisses() {
        when(redisService.getValues("key")).thenReturn(null);

        List<PlaceRecommendationItem> result = cacheService.getSearchResult("key");

        assertThat(result).isNull();
    }

    @Test
    void returnsSearchResultWhenCacheHits() {
        when(redisService.getValues("key")).thenReturn("""
                [
                  {
                    "placeId": "place-1",
                    "name": "라멘집",
                    "score": 100.0
                  }
                ]
                """);

        List<PlaceRecommendationItem> result = cacheService.getSearchResult("key");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).placeId()).isEqualTo("place-1");
    }

    @Test
    void deletesBrokenSearchCacheAndReturnsNull() {
        when(redisService.getValues("key")).thenReturn("{broken");

        List<PlaceRecommendationItem> result = cacheService.getSearchResult("key");

        assertThat(result).isNull();
        verify(redisService).deleteValues("key");
    }

    @Test
    void cachesPhotoMediaWithTtl() {
        cacheService.cachePhotoMedia("photo-key", new PlacePhotoMediaResponse("https://example.com/photo.jpg"));

        verify(redisService).setValues(eq("photo-key"), any(String.class), eq(Duration.ofMinutes(30)));
    }
}
