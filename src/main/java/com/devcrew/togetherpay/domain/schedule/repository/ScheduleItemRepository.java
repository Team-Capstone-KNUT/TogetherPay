package com.devcrew.togetherpay.domain.schedule.repository;

import com.devcrew.togetherpay.domain.schedule.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;

@Repository
public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    boolean existsByDateInAndSchedule_Trip_Id(Collection<LocalDate> dates, Long tripId);

}
