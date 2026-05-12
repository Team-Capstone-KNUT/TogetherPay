package com.devcrew.togetherpay.domain.trip.repository;

import com.devcrew.togetherpay.domain.trip.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findAllByTeamId(Long teamId);

    // 팀 아이디로 여행 목록 조회 및 시작일 순 정렬
    List<Trip> findByTeamIdOrderByStartDateAsc(Long teamID);

    Optional<Trip> findByIdAndTeamId(Long tripId, Long teamId);
}
