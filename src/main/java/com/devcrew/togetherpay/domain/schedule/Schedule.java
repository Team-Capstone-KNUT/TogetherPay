
package com.devcrew.togetherpay.domain.schedule;

import com.devcrew.togetherpay.domain.trip.Trip;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {
    @Id @Column(name = "schedule_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @OneToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Builder.Default
    @OneToMany(mappedBy = "schedule", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleItem>  scheduleItems = new ArrayList<>();

    public void splitSchedule() {
        scheduleItems.clear(); // 만에 하나 두번 다시 호출할 시 중복을 막기 위한..

        LocalDate currentDate = this.startDate;

        while(currentDate.isBefore(endDate.plusDays(1))) {
            ScheduleItem scheduleItem =
                    ScheduleItem.builder()
                    .date(currentDate)
                    .schedule(this).build();

            scheduleItems.add(scheduleItem);
            currentDate = currentDate.plusDays(1);
        }
    }


}
