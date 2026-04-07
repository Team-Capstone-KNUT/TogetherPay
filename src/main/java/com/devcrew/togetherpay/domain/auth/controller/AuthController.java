package com.devcrew.togetherpay.domain.auth.controller;

import com.devcrew.togetherpay.domain.auth.service.AuthService;
import com.devcrew.togetherpay.global.auth.jwt.JwtHeaderUtil;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * 사용자 로그아웃 로직
     */
    @PostMapping("logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletRequest request) {
        // 토큰 파싱 담긴 유틸리티 클래스 호출.
        String accessToken = JwtHeaderUtil.resolveToken(request);
        // 조건) 액세스 토큰이 비었을 경우 오류 발생
        if (accessToken == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // 그렇지 않은 경우 비즈니스 로직(로그아웃) 호출
        authService.logout(userId, accessToken);
        return ResponseEntity.ok().build();
    }

}
