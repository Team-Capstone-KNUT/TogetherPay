package com.devcrew.togetherpay.global.auth.oauth2;

import com.devcrew.togetherpay.domain.user.UserRole;
import com.devcrew.togetherpay.global.auth.jwt.TokenProvider;
import com.devcrew.togetherpay.global.redis.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final TokenProvider tokenProvider;
    private final RedisService redisService;

    @Value("${jwt.refresh-expiry-seconds}")
    private long refreshExpirySeconds;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.info("OAuth 로그인 성공. 프론트로 리다이렉트 준비");

        // 커스텀 객체에서 정보 가져오기
        CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();
        Long userId = customUser.getUserId();
        UserRole role = customUser.getRole();

        // 토큰 발급
        String accessToken = tokenProvider.createToken(userId, role);
        String refreshToken = tokenProvider.createRefreshToken(userId);

        // redis에 저장
        redisService.setValues("RT:" + userId, refreshToken, Duration.ofSeconds(refreshExpirySeconds));

        // 프론트엔드로 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

}
