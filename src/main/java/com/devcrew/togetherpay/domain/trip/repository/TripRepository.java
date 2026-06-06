package com.devcrew.togetherpay.domain.trip.repository;

import com.devcrew.togetherpay.domain.trip.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findAllByTeamId(Long teamId);

    // 팀 아이디로 여행 목록 조회 및 시작일 순 정렬
    List<Trip> findByTeamIdOrderByStartDateAsc(Long teamID);

    // trip에 user가 속하는지 검증 메서드
    boolean existsByIdAndTeam_TeamUsers_User_Id(Long tripId, Long userId);

    boolean existsByIdAndTeam_IdAndTeam_TeamUsers_User_Id(Long tripId, Long teamId, Long userId);
}
