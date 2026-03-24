package com.devcrew.togetherpay.global.auth.jwt;

import com.devcrew.togetherpay.global.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
/*
    메모) OncePerRequestFilter를 사용한 이유?
    원래 일반 Filter 인터페이스 사용해도 무관하지만, 클라이언트가 요청을 보냈을때 포워딩(요청을 다른 컨트롤러로 토스)
    이 일어나는 과정에서 필터가 2-3번 중복 실행되는 경우가 종종 생긴다.
    OncePerRequestFilter는 요청에 대해서 단 한번만 실행되는 것을 보장하는 필터라서 이걸 사용했다.
*/
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final RedisService redisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 프론트에서 보낸 요청 헤더에서 jwt 토큰을 추출한다.
        String token = resolveToken(request);
        // 토큰이 존재하고, tokenProvider에서 토큰 검증을 통과하면?
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            if (!redisService.hasKey(token)) {
                // 토큰을 분해해서 권한 정보(Authorization)을 만든다.
                Authentication authentication = tokenProvider.getAuthentication(token);
                // 스프링 시큐리티의 SecurityContext에 해당 유저의 권한 정보를 등록한다.
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // 디버깅 메세지 출력.
                log.debug("Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}", authentication.getName(), request.getRequestURI());
            } else {
                log.warn("블랙리스트 처리된 토큰으로 접근을 시도했습니다. uri: {}", request.getRequestURI());
            }
        }
        // 로직 종료 후, 다음 필터 아니면 컨트롤러로 요청을 넘긴다.
        filterChain.doFilter(request, response);
    }

    // Bearer 파싱 로직
    private String resolveToken(HttpServletRequest request) {
        // 프론트 요청으로부터 헤더를 가져온다.
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        // 헤더에 값이 있는지와 문자열이 "Bearer "로 시작하는지 검사한다.
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            // 조건 통과 시 "Bearer "(7글자)를 잘라서 토큰만 반환한다.
            return bearerToken.substring(7);
        }
        // 조건 미 통과시 null 반환됨. 오류 발생함.
        return null;
    }
}
