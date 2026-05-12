package com.devcrew.togetherpay.domain.schedule.dto.response;

import com.devcrew.togetherpay.domain.schedule.ScheduleItem;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ScheduleItemsResponse(
        List<ScheduleItemResponse> scheduleItemResponses
) {

    public static ScheduleItemsResponse from(List<ScheduleItem> scheduleItems) {
        List<ScheduleItemResponse> scheduleItemResponses = scheduleItems.stream()
                .map(item ->
                        {
                            return ScheduleItemResponse.builder()
                                    .scheduleItemId(item.getId())
                                    .scheduleId(item.getSchedule().getId())
                                    .date(item.getDate())
                                    .title(item.getTitle())
                                    .build();
                        }
                ).toList();

        return ScheduleItemsResponse.builder()
                .scheduleItemResponses(scheduleItemResponses)
                .build();
    }

    @Builder
    public record ScheduleItemResponse(
            Long scheduleItemId,
            Long scheduleId,
            LocalDate date,
            String title
    ) {}

}
