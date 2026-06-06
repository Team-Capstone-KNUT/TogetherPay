package com.devcrew.togetherpay.domain.schedule.repository;

import com.devcrew.togetherpay.domain.schedule.Schedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @EntityGraph(attributePaths = {"trip", "scheduleItems"})
    Optional<Schedule> findByTripId(long tripId);
}
