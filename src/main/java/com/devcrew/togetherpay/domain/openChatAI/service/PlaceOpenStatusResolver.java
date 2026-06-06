package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.response.OpenStatus;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class PlaceOpenStatusResolver {

    public OpenStatusResult resolve(OpeningHours openingHours, OffsetDateTime visitDateTime) {
        if (openingHours == null) {
            return new OpenStatusResult(
                    OpenStatus.UNKNOWN,
                    "Google Places에 영업시간 정보가 없어 방문 전 확인이 필요합니다.",
                    true
            );
        }

        if (visitDateTime == null) {
            if (openingHours.openNow() == null) {
                return new OpenStatusResult(
                        OpenStatus.UNKNOWN,
                        "현재 영업 여부 정보가 없어 방문 전 확인이 필요합니다.",
                        true
                );
            }

            return new OpenStatusResult(
                    Boolean.TRUE.equals(openingHours.openNow()) ? OpenStatus.LIKELY_OPEN : OpenStatus.LIKELY_CLOSED,
                    Boolean.TRUE.equals(openingHours.openNow())
                            ? "Google Places 기준 현재 영업 중으로 표시됩니다."
                            : "Google Places 기준 현재 영업 중으로 표시되지 않습니다.",
                    !Boolean.TRUE.equals(openingHours.openNow())
            );
        }

        if (openingHours.periods() == null || openingHours.periods().isEmpty()) {
            return new OpenStatusResult(
                    OpenStatus.UNKNOWN,
                    "방문 예정 시간 기준으로 판단할 영업시간 상세 정보가 없어 방문 전 확인이 필요합니다.",
                    true
            );
        }

        boolean openAtVisitTime = isOpenAt(openingHours.periods(), visitDateTime);

        return new OpenStatusResult(
                openAtVisitTime ? OpenStatus.LIKELY_OPEN : OpenStatus.LIKELY_CLOSED,
                openAtVisitTime
                        ? "Google Places 영업시간 기준 방문 예정 시간에 영업 중일 가능성이 높습니다."
                        : "Google Places 영업시간 기준 방문 예정 시간에 영업하지 않을 가능성이 있습니다.",
                !openAtVisitTime
        );
    }

    private boolean isOpenAt(List<Period> periods, OffsetDateTime visitDateTime) {
        int targetMinuteOfWeek = toGoogleMinuteOfWeek(visitDateTime);
        int weekMinutes = 7 * 24 * 60;

        for (Period period : periods) {
            if (period.open() == null) {
                continue;
            }

            int openMinute = toGoogleMinuteOfWeek(period.open());

            if (period.close() == null) {
                return true;
            }

            int closeMinute = toGoogleMinuteOfWeek(period.close());

            if (closeMinute <= openMinute) {
                closeMinute += weekMinutes;
            }

            if (openMinute <= targetMinuteOfWeek && targetMinuteOfWeek < closeMinute) {
                return true;
            }

            int shiftedTargetMinute = targetMinuteOfWeek + weekMinutes;
            if (openMinute <= shiftedTargetMinute && shiftedTargetMinute < closeMinute) {
                return true;
            }
        }

        return false;
    }

    private int toGoogleMinuteOfWeek(OffsetDateTime dateTime) {
        return toGoogleDay(dateTime.getDayOfWeek()) * 24 * 60
                + dateTime.getHour() * 60
                + dateTime.getMinute();
    }

    private int toGoogleMinuteOfWeek(TimeOfWeek timeOfWeek) {
        return timeOfWeek.day() * 24 * 60
                + timeOfWeek.hour() * 60
                + timeOfWeek.minute();
    }

    private int toGoogleDay(DayOfWeek dayOfWeek) {
        return dayOfWeek.getValue() % 7;
    }

    public record OpeningHours(
            Boolean openNow,
            List<String> weekdayDescriptions,
            List<Period> periods
    ) {
    }

    public record Period(TimeOfWeek open, TimeOfWeek close) {
    }

    public record TimeOfWeek(
            Integer day,
            Integer hour,
            Integer minute
    ) {
        public TimeOfWeek {
            if (day == null) {
                day = 0;
            }
            if (hour == null) {
                hour = 0;
            }
            if (minute == null) {
                minute = 0;
            }
        }
    }

    public record OpenStatusResult(
            OpenStatus status,
            String reason,
            boolean needsVerification
    ) {
    }
}
