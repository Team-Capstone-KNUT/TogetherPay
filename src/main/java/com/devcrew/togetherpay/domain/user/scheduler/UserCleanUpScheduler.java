package com.devcrew.togetherpay.domain.user.scheduler;

import com.devcrew.togetherpay.domain.user.UserStatus;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleanUpScheduler {
    private final UserRepository userRepository;
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanUpWithdrawnUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        log.info("탈퇴 후 30일 경과 회원 Hard Delete 배치 시작. 기준일: {}", threshold);
        userRepository.deleteByStatusAndUpdatedAtBefore(UserStatus.WITHDRAWN, threshold);
        log.info("배치 작업 완료.");
    }
}
