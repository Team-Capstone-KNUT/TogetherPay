package com.devcrew.togetherpay.domain.team.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddTeamMembersRequest(
        @NotEmpty(message = "추가할 멤버를 1명 이상 선택해주세요.")
        @Size(max = 50, message = "한 번에 최대 50명까지 추가할 수 있습니다.")
        List<@NotNull(message = "멤버 ID는 필수입니다.") Long> memberIds
) {}
