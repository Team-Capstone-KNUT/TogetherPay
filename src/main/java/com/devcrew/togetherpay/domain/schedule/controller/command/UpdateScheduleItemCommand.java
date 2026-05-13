package com.devcrew.togetherpay.domain.schedule.controller.command;

import lombok.Builder;

@Builder
public record UpdateScheduleItemCommand(
        String title,
        String description
) {
}
