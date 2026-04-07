package com.devcrew.togetherpay.domain.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTeamRequest(
        @NotBlank(message = "팀 이름 입력은 필수입니다.")
        @Size(max = 30, message = "팀 이름은 30자를 초과할 수 없습니다")
        String name
) {}
