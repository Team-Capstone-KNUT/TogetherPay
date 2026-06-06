package com.devcrew.togetherpay.domain.schedule.service;

import com.devcrew.togetherpay.domain.schedule.dto.response.ScheduleItemsResponse;
import com.devcrew.togetherpay.domain.schedule.repository.ScheduleItemRepository;
import com.devcrew.togetherpay.domain.schedule.repository.ScheduleRepository;
import com.devcrew.togetherpay.domain.team.repository.TeamRepository;
import com.devcrew.togetherpay.domain.trip.repository.TripRepository;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TripRepository tripRepository = mock(TripRepository.class);
    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final ScheduleItemRepository scheduleItemRepository = mock(ScheduleItemRepository.class);
    private final ScheduleService scheduleService = new ScheduleService(
            userRepository,
            teamRepository,
            tripRepository,
            scheduleRepository,
            scheduleItemRepository
    );

    @Test
    void returnsEmptyScheduleItemsWhenTripHasNoScheduleYet() {
        when(tripRepository.existsByIdAndTeam_TeamUsers_User_Id(2L, 1L)).thenReturn(true);
        when(scheduleRepository.findByTripId(2L)).thenReturn(Optional.empty());

        ScheduleItemsResponse response = scheduleService.getScheduleItems(1L, 2L);

        assertThat(response.scheduleItemResponses()).isEmpty();
        verify(scheduleRepository).findByTripId(2L);
    }

    @Test
    void rejectsScheduleItemsRequestWhenUserCannotAccessTrip() {
        when(tripRepository.existsByIdAndTeam_TeamUsers_User_Id(2L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.getScheduleItems(1L, 2L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_A_TRIP_USER));
    }
}
