package com.devcrew.togetherpay.domain.user.service;

import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.UserStatus;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    /**
     * 개인정보 조회
     */
    public User getMyProfile(Long userId) {
        return findUserOrThrow(userId);
    }

    /**
     * 닉네임 수정
     */
    @Transactional
    public void updateNickname(Long userId, String newNickname) {
        User user = findUserOrThrow(userId);
        user.updateNickname(newNickname);
        log.info("유저 닉네임 업데이트 완료, userId: {}", userId);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void withdraw(Long userId) {
        User user = findUserOrThrow(userId);

        // 이미 탈퇴한 회원인지 한번 더 검증
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            log.warn("이미 탈퇴된 회원에 대한 탈퇴 요청. userId: {}", userId);
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        // 상태를 WITHDRAWN으로 변경한다.
        user.withdraw();
        log.info("회원 탈퇴 처리 완료. userId: {}", userId);
    }

    /**
     * 공통 유저 조회 및 예외 처리 로직 분리
     */
    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

}
