package com.devcrew.togetherpay.global.auth.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class JwtHeaderUtil {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // 외부에서 생성자로 건들지 못하게 private 선언
    private JwtHeaderUtil() {
        throw new IllegalStateException("유틸리티 클래스");
    }

    // Bearer 파싱 로직(토큰 파싱 로직)
    public static String resolveToken(HttpServletRequest request) {
        // 프론트 요청으로부터 헤더를 가져온다.
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        // 조건) 헤더에 값이 있는지 여부와 문자열이 "Bearer "로 시작하는지를 검사한다.
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            // 조건 통과 시 앞의 BEARER_PREFIX만 잘라서 토큰만 반환한다.
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        // 조건 미 통과 -> null 바로 반환
        return null;
    }
}
