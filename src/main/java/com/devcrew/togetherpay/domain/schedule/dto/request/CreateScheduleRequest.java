package com.devcrew.togetherpay.domain.schedule.dto.request;

import com.devcrew.togetherpay.domain.schedule.controller.command.CreateScheduleCommand;
import lombok.Builder;

@Builder
public record CreateScheduleRequest(
        Long teamId,
        Long tripId
) {
    public CreateScheduleCommand toCommand() {
        return CreateScheduleCommand.builder()
                .teamId(teamId)
                .tripId(tripId)
                .build();
    }
}
