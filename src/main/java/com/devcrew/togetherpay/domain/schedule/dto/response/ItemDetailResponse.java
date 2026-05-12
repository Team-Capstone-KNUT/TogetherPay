package com.devcrew.togetherpay.domain.schedule.dto.response;

import com.devcrew.togetherpay.domain.schedule.ScheduleItem;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ItemDetailResponse(
        Long scheduleItemId,
        Long scheduleId,
        LocalDate date,
        String title,
        String description
) {
    public static ItemDetailResponse from(ScheduleItem scheduleItem) {
        return ItemDetailResponse.builder()
                .scheduleItemId(scheduleItem.getId())
                .scheduleId(scheduleItem.getSchedule().getId())
                .date(scheduleItem.getDate())
                .title(scheduleItem.getTitle())
                .description(scheduleItem.getDescription())
                .build();
    }
}
