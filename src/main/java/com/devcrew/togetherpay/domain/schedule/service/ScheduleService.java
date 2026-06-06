package com.devcrew.togetherpay.domain.schedule.service;

import com.devcrew.togetherpay.domain.schedule.Schedule;
import com.devcrew.togetherpay.domain.schedule.ScheduleItem;
import com.devcrew.togetherpay.domain.schedule.controller.command.CreateScheduleCommand;
import com.devcrew.togetherpay.domain.schedule.controller.command.UpdateScheduleItemCommand;
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
import lombok.NonNull;
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

        // 팀에 속하는지 검증
        validateTeamUser(user.getId(), command.teamId());
        validateTeamTrip(command.teamId(), command.tripId());

        Trip trip = getTrip(command.tripId());

        // 여행 일정 중복 검증
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
        validateTripUser(userId, tripId);

        return scheduleRepository.findByTripId(tripId)
                .map(schedule -> ScheduleItemsResponse.from(schedule.getScheduleItems()))
                .orElseGet(ScheduleItemsResponse::empty);
    }

    /**
     * 여행 일정 세부 조회
     * @param scheduleItemId
     * @return ItemDetailResponse
     */
    @Transactional(readOnly = true)
    public ItemDetailResponse findScheduleItem(Long scheduleItemId) {
        ScheduleItem scheduleItem = getScheduleItemOrElseThrow(scheduleItemId);

        return ItemDetailResponse.from(scheduleItem);
    }

    /**
     * 여행 일정 수정 & 등록
     * @param userId
     * @param tripId
     * @param scheduleItemId
     */
    public void update(Long userId, Long tripId, Long scheduleItemId, UpdateScheduleItemCommand command) {
        Trip trip = getTrip(tripId);

        // 여행에 사용자가 속하는지 검증
        validateTripUser(userId, tripId);

        ScheduleItem scheduleItem =
                getScheduleItemOrElseThrow(scheduleItemId);

        Set<LocalDate> dates = calculateDate(trip);

        // 세부 일정이 포함하는지 검증
        validateScheduleItemDate(dates, scheduleItem);

        scheduleItem.update(command.title(), command.description());

    }

    /**
     * 여행 일정 삭제(내용만 null로)
     * @param userId
     * @param tripId
     * @param scheduleItemId
     */
    public void delete(Long userId, Long tripId, Long scheduleItemId) {
        validateTripUser(userId, tripId);

        ScheduleItem scheduleItem =
                getScheduleItemOrElseThrow(scheduleItemId);

        scheduleItem.delete();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateTeamUser(Long userId, Long teamId) {
        if(!teamRepository.existsByIdAndTeamUsers_userId(teamId, userId)) {
            throw new BusinessException(ErrorCode.NOT_A_TEAM_USER);
        }
    }

    private void validateTeamTrip(Long teamId, Long tripId) {
        if(!teamRepository.existsByIdAndTrips_Id(teamId, tripId)) {
            throw new BusinessException(ErrorCode.NOT_A_TEAM_TRIP);
        }
    }

    private Team getTeam(CreateScheduleCommand command) {
        return teamRepository.findById(command.teamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private Trip getTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    }

    private void isDuplicateSchedule(Trip trip) {
        Set<LocalDate> dates = calculateDate(trip);

        if(scheduleItemRepository.existsByDateInAndSchedule_Trip_Id(dates, trip.getId())) {
            throw new BusinessException(ErrorCode.SCHEDULE_ALREADY_EXISTS);
        }
    }

    // 날짜 계산 후 집합에 담기
    private Set<LocalDate> calculateDate(Trip trip) {
        LocalDate currentDate = trip.getStartDate();
        LocalDate endDate = trip.getEndDate();

        Set<LocalDate> dates = new HashSet<>();

        while(currentDate.isBefore(endDate.plusDays(1))) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        return dates;
    }

    private void validateTripUser(Long userId, Long tripId) {
        // trip에 user가 속하는지 검증 메서드
        if(!tripRepository.existsByIdAndTeam_TeamUsers_User_Id(tripId, userId)) {
            throw new BusinessException(ErrorCode.NOT_A_TRIP_USER);
        }
    }

    private ScheduleItem getScheduleItemOrElseThrow(Long scheduleItemId) {
        return scheduleItemRepository.findById(scheduleItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_ITEM_NOT_FOUND));
    }

    private void validateScheduleItemDate(Set<LocalDate> dates, ScheduleItem scheduleItem) {
        if(!dates.contains(scheduleItem.getDate())) {
            throw new BusinessException(ErrorCode.INVALID_SCHEDULE_DATE);
        }
    }

}
