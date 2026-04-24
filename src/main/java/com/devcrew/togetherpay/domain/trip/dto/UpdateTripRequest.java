package com.devcrew.togetherpay.domain.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateTripRequest(
        @NotBlank(message = "여행 제목을 입력해주세요.")
        String title,

        @NotNull(message = "여행 시작일을 선택해주세요.")
        LocalDate startDate,

        @NotNull(message = "여행 종료일을 선택해주세요.")
        LocalDate endDate
) {
}
