package com.devcrew.togetherpay.domain.auth.service;

import com.devcrew.togetherpay.global.auth.jwt.TokenProvider;
import com.devcrew.togetherpay.global.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenProvider tokenProvider;
    private final RedisService redisService;

    @Transactional
    public void logout(Long userId, String accessToken) {
        String refreshTokenKey = "RT:" + userId;
        if (redisService.hasKey(refreshTokenKey)) {
            redisService.deleteValues(refreshTokenKey);
            log.debug("Redis에서 Refresh Token 삭제 완료. userId: {}", userId);
        }

        Long expirationTime = tokenProvider.getExpiration(accessToken);
        long remainingTime = expirationTime - System.currentTimeMillis();

        if (remainingTime > 0) {
            redisService.setValues(accessToken, "logout", Duration.ofMillis(remainingTime));
            log.info("Access Token 블랙리스트 등록 완료. 남은 시간(ms): {}", remainingTime);
        }
    }
}
