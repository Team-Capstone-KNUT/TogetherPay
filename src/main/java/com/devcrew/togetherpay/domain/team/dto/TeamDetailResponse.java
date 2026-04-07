package com.devcrew.togetherpay.domain.team.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record TeamDetailResponse(
        Long teamId,
        String name,
        String inviteCode,
        List<MemberResponse> members
) {}

