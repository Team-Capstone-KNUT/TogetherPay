package com.devcrew.togetherpay.domain.schedule.repository;

import com.devcrew.togetherpay.domain.schedule.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findByIdAndTripId(Long scheduleId, Long tripId);


    Optional<Schedule> findByTripId(long tripId);
}
