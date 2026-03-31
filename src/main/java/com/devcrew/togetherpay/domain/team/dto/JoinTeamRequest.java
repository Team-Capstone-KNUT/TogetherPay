package com.devcrew.togetherpay.domain.team.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinTeamRequest(
        @NotBlank(message = "초대 코드는 필수 입력입니다.")
        String inviteCode,
        @NotBlank(message = "팀 비밀번호는 필수 입력입니다.")
        String password
) {
}
