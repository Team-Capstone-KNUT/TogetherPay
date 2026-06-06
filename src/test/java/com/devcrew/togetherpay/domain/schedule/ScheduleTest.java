package com.devcrew.togetherpay.domain.schedule;

import com.devcrew.togetherpay.domain.schedule.dto.request.UpdateScheduleItemRequest;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void addsMissingScheduleItemWhenTripDateRangeIsExtended() {
        LocalDate startDate = LocalDate.of(2026, 6, 5);
        Schedule schedule = Schedule.builder()
                .startDate(startDate)
                .endDate(LocalDate.of(2026, 6, 6))
                .build();
        schedule.splitSchedule();

        schedule.syncDateRange(startDate, LocalDate.of(2026, 6, 7));

        assertThat(schedule.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 7));
        assertThat(schedule.getScheduleItems())
                .extracting(ScheduleItem::getDate)
                .containsExactly(
                        LocalDate.of(2026, 6, 5),
                        LocalDate.of(2026, 6, 6),
                        LocalDate.of(2026, 6, 7)
                );
    }

    @Test
    void removesEmptyScheduleItemsOutsideShortenedDateRange() {
        LocalDate startDate = LocalDate.of(2026, 6, 5);
        Schedule schedule = Schedule.builder()
                .startDate(startDate)
                .endDate(LocalDate.of(2026, 6, 7))
                .build();
        schedule.splitSchedule();

        schedule.syncDateRange(startDate, LocalDate.of(2026, 6, 6));

        assertThat(schedule.getScheduleItems())
                .extracting(ScheduleItem::getDate)
                .containsExactly(
                        LocalDate.of(2026, 6, 5),
                        LocalDate.of(2026, 6, 6)
                );
    }

    @Test
    void rejectsShortenedDateRangeWhenRemovedScheduleItemHasContent() {
        LocalDate startDate = LocalDate.of(2026, 6, 5);
        ScheduleItem populatedItem = ScheduleItem.builder()
                .date(LocalDate.of(2026, 6, 7))
                .title("오사카성")
                .description("오전 방문")
                .build();
        Schedule schedule = Schedule.builder()
                .startDate(startDate)
                .endDate(LocalDate.of(2026, 6, 7))
                .scheduleItems(List.of(
                        ScheduleItem.builder().date(startDate).build(),
                        ScheduleItem.builder().date(LocalDate.of(2026, 6, 6)).build(),
                        populatedItem
                ))
                .build();

        assertThatThrownBy(() -> schedule.syncDateRange(startDate, LocalDate.of(2026, 6, 6)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_DATE_RANGE_CONFLICT));
    }

    @Test
    void acceptsDetailedScheduleDescriptionWithinLimit() {
        UpdateScheduleItemRequest request = new UpdateScheduleItemRequest(
                "도쿄 첫날",
                "상세 일정 ".repeat(500)
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsScheduleDescriptionOverLimit() {
        UpdateScheduleItemRequest request = new UpdateScheduleItemRequest(
                "도쿄 첫날",
                "가".repeat(10001)
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("description");
    }
}
