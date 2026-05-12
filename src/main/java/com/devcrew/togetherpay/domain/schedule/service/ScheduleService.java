package com.devcrew.togetherpay.domain.schedule.service;

import com.devcrew.togetherpay.domain.schedule.Schedule;
import com.devcrew.togetherpay.domain.schedule.ScheduleItem;
import com.devcrew.togetherpay.domain.schedule.controller.command.CreateScheduleCommand;
import com.devcrew.togetherpay.domain.schedule.dto.response.ItemDetailResponse;
import com.devcrew.togetherpay.domain.schedule.dto.response.ScheduleItemsResponse;
import com.devcrew.togetherpay.domain.schedule.repository.ScheduleItemRepository;
import com.devcrew.togetherpay.domain.schedule.repository.ScheduleRepository;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.repository.TeamRepository;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.domain.trip.repository.TripRepository;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
public class ScheduleService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TripRepository tripRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleItemRepository scheduleItemRepository;

    /**
     * 여행 일정 생성
     * @param userId
     * @param command(teamId, tripId)
     */
    public void create(Long userId, CreateScheduleCommand command) {
        User user = getUser(userId);

        validateTeamUser(user.getId(), command);

        Team team = getTeam(command);
        Trip trip = getTrip(command);

        isDuplicateSchedule(trip);

        Schedule schedule = command.toEntity(trip);

        schedule.splitSchedule();

        scheduleRepository.save(schedule);
    }

    /**
     * 여행 단위로 일정 조회
     * @param userId
     * @param tripId
     * @return ScheduleItemsResponse
     */
    @Transactional(readOnly = true)
    public ScheduleItemsResponse getScheduleItems(Long userId, Long tripId) {
        Schedule schedule = scheduleRepository.findByIdAndTripId(userId, tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        return ScheduleItemsResponse.from(schedule.getScheduleItems());
    }

    /**
     * 여행 일정 세부 조회
     * @param scheduleItemId
     * @return ItemDetailResponse
     */
    @Transactional(readOnly = true)
    public ItemDetailResponse getScheduleItem(Long scheduleItemId) {
        ScheduleItem scheduleItem = scheduleItemRepository.findById(scheduleItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_ITEM_NOT_FOUND));

        return ItemDetailResponse.from(scheduleItem);
    }

    /**
     * 여행 일정 수정 & 등록
     * @param userId
     * @param scheduleItemId
     */
    public void update(Long userId, Long scheduleItemId) {

    }



    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateTeamUser(Long userId, CreateScheduleCommand command) {
        if(!teamRepository.existsByIdAndTeamUsers_userId(command.teamId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_A_TEAM_USER);
        }
    }

    private Team getTeam(CreateScheduleCommand command) {
        return teamRepository.findById(command.teamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private Trip getTrip(CreateScheduleCommand command) {
        return tripRepository.findByIdAndTeamId(command.tripId(), command.teamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    }

    private void isDuplicateSchedule(Trip trip) {
        LocalDate currentDate = trip.getStartDate();
        LocalDate endDate = trip.getEndDate();

        Set<LocalDate> dates = new HashSet<>();

        while(currentDate.isBefore(endDate.plusDays(1))) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        if(scheduleItemRepository.existsByDateInAndSchedule_Trip_Id(dates, trip.getId())) {
            throw new BusinessException(ErrorCode.SCHEDULE_ALREADY_EXISTS);
        }
    }

}
