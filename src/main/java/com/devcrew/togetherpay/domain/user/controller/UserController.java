package com.devcrew.togetherpay.domain.user.controller;

import com.devcrew.togetherpay.domain.user.dto.UpdateNicknameRequest;
import com.devcrew.togetherpay.domain.user.dto.UserProfileResponse;
import com.devcrew.togetherpay.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private UserService userService;

    /**
     * 정보 조회(프로필 조회)
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        UserProfileResponse response = UserProfileResponse.from(userService.getMyProfile(userId));
        return ResponseEntity.ok(response);
    }

    /**
     * 닉네임 수정
     */
    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNicknameRequest request) {

        userService.updateNickname(userId, request.nickname());
        return ResponseEntity.ok().build();
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.ok().build();
    }

}
