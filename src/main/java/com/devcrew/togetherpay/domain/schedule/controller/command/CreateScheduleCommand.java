package com.devcrew.togetherpay.domain.schedule.controller.command;

import com.devcrew.togetherpay.domain.schedule.Schedule;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.trip.Trip;
import lombok.Builder;

@Builder
public record CreateScheduleCommand(
        Long teamId,
        Long tripId
) {

    public Schedule toEntity(Trip trip) {
        return Schedule.builder()
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .trip(trip)
                .build();
    }



}
