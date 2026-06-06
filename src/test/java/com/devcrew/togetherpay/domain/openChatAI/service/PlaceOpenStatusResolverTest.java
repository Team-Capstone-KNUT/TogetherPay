package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.response.OpenStatus;
import com.devcrew.togetherpay.domain.openChatAI.service.PlaceOpenStatusResolver.OpenStatusResult;
import com.devcrew.togetherpay.domain.openChatAI.service.PlaceOpenStatusResolver.OpeningHours;
import com.devcrew.togetherpay.domain.openChatAI.service.PlaceOpenStatusResolver.Period;
import com.devcrew.togetherpay.domain.openChatAI.service.PlaceOpenStatusResolver.TimeOfWeek;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOpenStatusResolverTest {

    private final PlaceOpenStatusResolver resolver = new PlaceOpenStatusResolver();

    @Test
    void returnsLikelyOpenWhenVisitTimeIsInsideSameDayPeriod() {
        OpeningHours openingHours = openingHours(period(2, 10, 0, 2, 22, 0));
        OffsetDateTime visitDateTime = OffsetDateTime.parse("2026-06-02T19:30:00+09:00");

        OpenStatusResult result = resolver.resolve(openingHours, visitDateTime);

        assertThat(result.status()).isEqualTo(OpenStatus.LIKELY_OPEN);
        assertThat(result.needsVerification()).isFalse();
    }

    @Test
    void returnsLikelyClosedWhenVisitTimeIsOutsidePeriod() {
        OpeningHours openingHours = openingHours(period(2, 10, 0, 2, 15, 0));
        OffsetDateTime visitDateTime = OffsetDateTime.parse("2026-06-02T19:30:00+09:00");

        OpenStatusResult result = resolver.resolve(openingHours, visitDateTime);

        assertThat(result.status()).isEqualTo(OpenStatus.LIKELY_CLOSED);
        assertThat(result.needsVerification()).isTrue();
    }

    @Test
    void handlesOverMidnightPeriod() {
        OpeningHours openingHours = openingHours(period(2, 22, 0, 3, 2, 0));
        OffsetDateTime visitDateTime = OffsetDateTime.parse("2026-06-03T01:00:00+09:00");

        OpenStatusResult result = resolver.resolve(openingHours, visitDateTime);

        assertThat(result.status()).isEqualTo(OpenStatus.LIKELY_OPEN);
        assertThat(result.needsVerification()).isFalse();
    }

    @Test
    void returnsUnknownWhenOpeningHoursAreMissing() {
        OpenStatusResult result = resolver.resolve(null, OffsetDateTime.parse("2026-06-02T19:30:00+09:00"));

        assertThat(result.status()).isEqualTo(OpenStatus.UNKNOWN);
        assertThat(result.needsVerification()).isTrue();
    }

    @Test
    void usesOpenNowWhenVisitTimeIsMissing() {
        OpeningHours openingHours = new OpeningHours(true, List.of(), List.of());

        OpenStatusResult result = resolver.resolve(openingHours, null);

        assertThat(result.status()).isEqualTo(OpenStatus.LIKELY_OPEN);
        assertThat(result.needsVerification()).isFalse();
    }

    @Test
    void treatsPeriodWithoutCloseTimeAsAlwaysOpenFromOpenTime() {
        OpeningHours openingHours = new OpeningHours(
                null,
                List.of(),
                List.of(new Period(new TimeOfWeek(2, 0, 0), null))
        );
        OffsetDateTime visitDateTime = OffsetDateTime.parse("2026-06-05T23:00:00+09:00");

        OpenStatusResult result = resolver.resolve(openingHours, visitDateTime);

        assertThat(result.status()).isEqualTo(OpenStatus.LIKELY_OPEN);
        assertThat(result.needsVerification()).isFalse();
    }

    private OpeningHours openingHours(Period period) {
        return new OpeningHours(null, List.of(), List.of(period));
    }

    private Period period(
            int openDay,
            int openHour,
            int openMinute,
            int closeDay,
            int closeHour,
            int closeMinute
    ) {
        return new Period(
                new TimeOfWeek(openDay, openHour, openMinute),
                new TimeOfWeek(closeDay, closeHour, closeMinute)
        );
    }
}
