package com.devcrew.togetherpay.domain.schedule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schedule_item")
public class ScheduleItem {
    @Id
    @Column(name = "schedule_item_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = true, length = 200)
    private String title;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void delete() {
        this.title = null;
        this.description = null;
    }

    public boolean hasContent() {
        return title != null && !title.isBlank()
                || description != null && !description.isBlank();
    }
}
