package com.devcrew.togetherpay.domain.team.dto;

import com.devcrew.togetherpay.domain.team.TeamRole;
import lombok.Builder;

@Builder
public record MemberResponse(
        Long userId,
        String nickname,
        TeamRole role
) {}