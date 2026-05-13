package com.devcrew.togetherpay.domain.schedule.dto.request;

import com.devcrew.togetherpay.domain.schedule.controller.command.UpdateScheduleItemCommand;
import lombok.Builder;

@Builder
public record UpdateScheduleItemRequest(
        String title,
        String description
) {

    public UpdateScheduleItemCommand toCommand() {
        return UpdateScheduleItemCommand.builder()
                .title(title)
                .description(description)
                .build();
    }
}
