package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.schedule.Schedule;
import com.devcrew.togetherpay.domain.schedule.ScheduleItem;
import com.devcrew.togetherpay.domain.schedule.repository.ScheduleRepository;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TravelContextService {

    private final ScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public String buildScheduleSummaryContext(long tripId) {
        Schedule schedule = scheduleRepository.findByTripId(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        Trip trip = schedule.getTrip();

        StringBuilder sb = new StringBuilder();
        sb.append("여행명: ")
                .append(trip.getTitle())
                .append("\n\n");

        sb.append("사용자가 등록한 여행 일정입니다.\n\n");

        for (ScheduleItem item : schedule.getScheduleItems()) {
            sb.append("[")
                    .append(item.getDate())
                    .append("]\n");

            sb.append("제목: ")
                    .append(item.getTitle() == null ? "없음" : item.getTitle())
                    .append("\n");

            sb.append("설명: ")
                    .append(item.getDescription() == null ? "없음" : item.getDescription())
                    .append("\n\n");
        }

        return sb.toString();
    }
}
