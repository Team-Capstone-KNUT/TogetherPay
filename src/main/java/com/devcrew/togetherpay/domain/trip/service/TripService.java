package com.devcrew.togetherpay.domain.trip.service;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.domain.team.repository.TeamRepository;
import com.devcrew.togetherpay.domain.team.repository.TeamUserRepository;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.domain.trip.dto.TripResponse;
import com.devcrew.togetherpay.domain.trip.dto.UpdateTripRequest;
import com.devcrew.togetherpay.domain.trip.dto.command.CreateTripCommand;
import com.devcrew.togetherpay.domain.trip.repository.TripRepository;
import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRepository;
import com.devcrew.togetherpay.global.common.vo.Money;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
public class TripService {

    private final TripRepository tripRepository;
    private final TeamRepository teamRepository;
    private final TeamUserRepository teamUserRepository;
    private final UserRepository userRepository;

    public TripResponse createTrip(Long userId, CreateTripCommand command) {
        // 여행 시작일이 종료일보다 늦다면 예외 발생
        if (command.startDate().isAfter(command.endDate())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        // [팀 검증] 해당 팀이 존재하지 않는 경우 예외 발생
        Team team = getTeamOrThrow(command.teamId());

        // [권한 검증] 팀 멤버만 여행을 생성할 수 있음
        validateUserIsTeamMember(userId, team);

        // toEntity(team)으로 여행 엔티티 생성
        Trip trip = command.toEntity(team);

        // [핵심 로직] 여행 기간에 맞춰 일자별로 0원의 예산을 생성
        long daysBetween = ChronoUnit.DAYS.between(command.startDate(), command.endDate());

        for (int i = 0; i <= daysBetween; i++) {
            LocalDate budgetDate = command.startDate().plusDays(i);

            Budget dailyBudget = Budget.createDailyBudget(
                    trip,
                    budgetDate,
                    Money.wons(0L)
            );

            trip.getBudgets().add(dailyBudget);
        }

        Trip savedTrip = tripRepository.save(trip);

        return TripResponse.from(savedTrip);
    }

    /**
     * 여행 상세 조회
     * @param userId
     * @param tripId
     * @return
     */
    @Transactional(readOnly = true)
    public TripResponse getTrip(Long userId, Long tripId) {
        Trip trip = getTripOrThrow(tripId);
        validateUserIsTeamMember(userId, trip.getTeam());

        return TripResponse.from(trip);
    }

    /**
     * 특정 팀의 모든 여행 목록 조회
     * @param userId
     * @param teamId
     * @return
     */
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByTeam(Long userId, Long teamId) {
        // 팀 존재 여부 검증
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        // 멤버 권한 검증
        validateUserIsTeamMember(userId, team);

        List<Trip> trips = tripRepository.findByTeamIdOrderByStartDateAsc(teamId);
        return trips.stream().map(TripResponse::from).toList();
    }

    /**
     * 여행 수정
     * @param userId
     * @param tripId
     * @param request
     * @return
     */
    public TripResponse updateTrip(Long userId, Long tripId, UpdateTripRequest request) {
        Trip trip = getTripOrThrow(tripId);
        validateUserIsTeamMember(userId, trip.getTeam());

        // 시작일이 종료일보다 늦은지를 검증
        if (request.startDate().isAfter(request.endDate())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        trip.updateInfo(request.title(), request.startDate(), request.endDate());

        return TripResponse.from(trip);
    }

    /**
     * 여행 삭제
     * @param userId
     * @param tripId
     */
    public void deleteTrip(Long userId, Long tripId) {
        Trip trip = getTripOrThrow(tripId);

        validateUserIsTeamMember(userId, trip.getTeam());

        tripRepository.delete(trip);
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Trip getTripOrThrow(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    }

    private void validateUserIsTeamMember(Long userId, Team team) {
        User user = getUserOrThrow(userId);
        if (!teamUserRepository.existsByTeamAndUser(team, user)) {
            throw new BusinessException(ErrorCode.NOT_A_TEAM_USER);
        }
    }
}
