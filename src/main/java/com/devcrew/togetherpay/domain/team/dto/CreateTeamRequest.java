package com.devcrew.togetherpay.domain.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTeamRequest(
        @NotBlank(message = "팀 이름은 필수 입력입니다.")
        @Size(max = 50, message = "팀 이름은 50자를 초과할 수 없습니다.")
        String name,

        @NotBlank(message = "팀 비밀번호는 필수 입력입니다.")
        String password,

        // 팀 생성 시 함께 초대한 멤버들 ID 목록(선택)
        List<Long> memberIds
) {}
